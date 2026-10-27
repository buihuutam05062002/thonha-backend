package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.*;
import com.thonha.backend.repository.MatchingLogRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.ReviewRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final WorkerProfileRepository workerProfileRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final MatchingLogRepository matchingLogRepository;
    private final ReviewRepository reviewRepository;
    private final GoongService goongService;
    private final NotificationService notificationService;
    private final WorkerLoadService workerLoadService;
    private final WorkerLocationService workerLocationService;
    private final WalletService walletService;

    private static final BigDecimal WEIGHT_DISTANCE = new BigDecimal("0.40");      
    private static final BigDecimal WEIGHT_RATING = new BigDecimal("0.25");        
    private static final BigDecimal WEIGHT_ACCEPTANCE = new BigDecimal("0.20");    
    private static final BigDecimal WEIGHT_WORKLOAD = new BigDecimal("0.15");      

    public static final int MATCHING_TIMEOUT_MINUTES = 5;
    private static final int MAX_WORKERS_PER_REQUEST = 5;
    private static final int MAX_DISTANCE_KM = 20;
    private static final int MAX_CANDIDATES = 20;

    private static final BigDecimal NEUTRAL_SCORE = BigDecimal.valueOf(50);
    static final int ACCEPTANCE_WINDOW_DAYS = 30;
    private static final int MIN_RESPONSES_FOR_ACCEPTANCE = 5;
    static final List<MatchingResult> RESPONSE_RESULTS =
            List.of(MatchingResult.ACCEPTED, MatchingResult.REJECTED, MatchingResult.EXPIRED);

    private static boolean isOpenForMatching(RepairRequest request) {
        return request.getStatus() == RepairStatus.PENDING_MATCH || request.getStatus() == RepairStatus.MATCHING;
    }

    @Transactional
    public MatchingResultDTO findAndMatchWorkers(Long requestId) {

        repairRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));

        if (request.getStatus() != RepairStatus.PENDING_MATCH && request.getStatus() != RepairStatus.MATCHING
                && request.getStatus() != RepairStatus.NOT_FOUND) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Yêu cầu không ở trạng thái chờ ghép thợ");
        }

        if (request.getLat() == null || request.getLng() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Yêu cầu thiếu tọa độ vị trí");
        }

        request.setStatus(RepairStatus.MATCHING);
        request.setUpdatedAt(LocalDateTime.now());

        Candidates candidates = findCandidates(request);

        Set<Long> triedWorkerIds = matchingLogRepository.findByRequestId(requestId, Pageable.unpaged())
                .stream().map(l -> l.getWorker().getId()).collect(Collectors.toSet());
        List<WorkerProfile> availableWorkers = candidates.workers().stream()
                .filter(w -> !triedWorkerIds.contains(w.getId()))
                .toList();
        if (availableWorkers.isEmpty()) {
            request.setStatus(RepairStatus.NOT_FOUND);
            return MatchingResultDTO.builder()
                    .success(false)
                    .message("Không tìm thấy thợ phù hợp")
                    .matchedWorkers(Collections.emptyList())
                    .build();
        }

        List<WorkerScoreDTO> scoredWorkers = calculateScores(request, availableWorkers, candidates.locations());
        
        List<WorkerScoreDTO> eligibleWorkers = scoredWorkers.stream()
                .filter(w -> w.getDistanceKm() <= MAX_DISTANCE_KM)
                .limit(MAX_WORKERS_PER_REQUEST)
                .toList();

        if (eligibleWorkers.isEmpty()) {
            request.setStatus(RepairStatus.NOT_FOUND);
            return MatchingResultDTO.builder()
                    .success(false)
                    .message("Không có thợ nào trong khoảng cách cho phép")
                    .matchedWorkers(Collections.emptyList())
                    .build();
        }

        request.setStatus(RepairStatus.MATCHING);
        
        List<WorkerMatchDTO> matchedWorkers = new ArrayList<>();
        for (WorkerScoreDTO scored : eligibleWorkers) {
            MatchingLog log = createMatchingLog(request, scored.getWorker(), scored);
            matchingLogRepository.save(log);

            notificationService.sendMatchingRequest(scored.getWorker().getUser().getId(), request.getId(), log.getId());

            matchedWorkers.add(WorkerMatchDTO.builder()
                    .workerId(scored.getWorker().getId())
                    .workerName(scored.getWorker().getUser().getFullName())
                    .distanceKm(scored.getDistanceKm())
                    .durationMinutes(scored.getDurationMinutes())
                    .totalScore(scored.getTotalScore())
                    .matchingLogId(log.getId())
                    .build());
        }

        return MatchingResultDTO.builder()
                .success(true)
                .message("Đã gửi yêu cầu đến " + matchedWorkers.size() + " thợ")
                .matchedWorkers(matchedWorkers)
                .build();
    }

    private record Candidates(List<WorkerProfile> workers, Map<Long, WorkerLocationService.NearbyWorker> locations) {
        static Candidates empty() {
            return new Candidates(List.of(), Map.of());
        }
    }


    private Candidates findCandidates(RepairRequest request) {
        List<WorkerLocationService.NearbyWorker> nearby;
        try {
            nearby = workerLocationService.findNearby(
                    request.getLat().doubleValue(), request.getLng().doubleValue(),
                    MAX_DISTANCE_KM, MAX_CANDIDATES);
        } catch (RuntimeException e) {
            log.error("Không đọc được vị trí thợ từ Redis", e);
            return Candidates.empty();
        }
        if (nearby.isEmpty()) return Candidates.empty();

        Map<Long, WorkerLocationService.NearbyWorker> locations = nearby.stream()
                .collect(Collectors.toMap(WorkerLocationService.NearbyWorker::workerProfileId, w -> w, (a, b) -> a));

        List<WorkerProfile> workers = workerProfileRepository.findAvailableWorkersByIds(
                locations.keySet(),
                ApprovalStatus.APPROVED,
                AvailabilityStatus.READY,
                request.getCategory().getId(),
                request.getCustomer().getId(),
                walletService.getMinBalanceToReceiveJobs());
        return new Candidates(workers, locations);
    }

    private List<WorkerScoreDTO> calculateScores(RepairRequest request, List<WorkerProfile> workers,
                                                 Map<Long, WorkerLocationService.NearbyWorker> locations) {
        
        GoongService.Coordinate customerCoord = new GoongService.Coordinate(
                request.getLat().doubleValue(),
                request.getLng().doubleValue()
        );

        
        List<GoongService.Coordinate> workerCoords = workers.stream()
                .map(w -> {
                    WorkerLocationService.NearbyWorker loc = locations.get(w.getId());
                    return new GoongService.Coordinate(loc.lat(), loc.lng());
                })
                .toList();

        
        GoongService.DistanceMatrixResponse distanceMatrix = goongService.getDistanceMatrix(
                List.of(GoongService.Coordinate.from(request.getLat(), request.getLng())),
                workerCoords,
                "bike" 
        );

        
        
        Set<Long> workerIds = workers.stream().map(WorkerProfile::getId).collect(Collectors.toSet());
        Map<Long, double[]> reviewStats = loadReviewStats(workerIds);       
        Map<Long, long[]> responseStats = loadResponseStats(workerIds);     

        List<WorkerScoreDTO> scoredWorkers = new ArrayList<>();

        for (int i = 0; i < workers.size() && i < distanceMatrix.getRows().get(0).getElements().size(); i++) {
            WorkerProfile worker = workers.get(i);
            GoongService.DistanceMatrixResponse.Element element = 
                    distanceMatrix.getRows().get(0).getElements().get(i);

            if (!"OK".equals(element.getStatus()) || element.getDistance() == null) {
                log.warn("Không tính được khoảng cách cho thợ {}", worker.getId());
                continue;
            }

            double distanceKm = element.getDistance().getValue() / 1000.0;
            int durationMinutes = (int) Math.ceil(element.getDuration().getValue() / 60.0);

            
            BigDecimal distanceScore = calculateDistanceScore(distanceKm);
            double[] review = reviewStats.getOrDefault(worker.getId(), new double[]{0, 0});
            long[] response = responseStats.getOrDefault(worker.getId(), new long[]{0, 0});
            BigDecimal ratingScore = calculateRatingScore(review[0], (long) review[1]);
            BigDecimal acceptanceScore = calculateAcceptanceRateScore(response[0], response[1]);
            BigDecimal workloadScore = calculateWorkloadScore(worker.getOngoingJobsCount());

            
            BigDecimal totalScore = distanceScore.multiply(WEIGHT_DISTANCE)
                    .add(ratingScore.multiply(WEIGHT_RATING))
                    .add(acceptanceScore.multiply(WEIGHT_ACCEPTANCE))
                    .add(workloadScore.multiply(WEIGHT_WORKLOAD))
                    .setScale(4, RoundingMode.HALF_UP);

            scoredWorkers.add(WorkerScoreDTO.builder()
                    .worker(workers.get(i))
                    .distanceKm(element.getDistance().getValue() / 1000.0)
                    .durationMinutes((int) Math.ceil(element.getDuration().getValue() / 60.0))
                    .distanceScore(distanceScore)
                    .ratingScore(ratingScore)
                    .acceptanceScore(acceptanceScore)
                    .workloadScore(workloadScore)
                    .totalScore(totalScore)
                    .build());
        }

        
        return scoredWorkers.stream()
                .sorted(Comparator.comparing(WorkerScoreDTO::getTotalScore).reversed())
                .toList();
    }

    private BigDecimal calculateDistanceScore(double distanceKm) {
        
        if (distanceKm <= 1) return BigDecimal.valueOf(100);
        if (distanceKm >= MAX_DISTANCE_KM) return BigDecimal.ZERO;
        return BigDecimal.valueOf(100 * (1 - (distanceKm - 1) / (MAX_DISTANCE_KM - 1)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRatingScore(double avgRating, long reviewCount) {
        
        if (reviewCount == 0) return NEUTRAL_SCORE;
        return BigDecimal.valueOf(avgRating * 20).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAcceptanceRateScore(long accepted, long responded) {
        
        if (responded < MIN_RESPONSES_FOR_ACCEPTANCE) return NEUTRAL_SCORE;
        return BigDecimal.valueOf(accepted * 100.0 / responded).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, double[]> loadReviewStats(Collection<Long> workerIds) {
        Map<Long, double[]> stats = new HashMap<>();
        for (Object[] row : reviewRepository.summarizeByWorkers(workerIds)) {
            stats.put((Long) row[0], new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue()});
        }
        return stats;
    }

    /** Chấp nhận / (chấp nhận + từ chối + bỏ lỡ) trong ACCEPTANCE_WINDOW_DAYS ngày gần nhất. */
    private Map<Long, long[]> loadResponseStats(Collection<Long> workerIds) {
        LocalDateTime since = LocalDateTime.now().minusDays(ACCEPTANCE_WINDOW_DAYS);
        Map<Long, long[]> stats = new HashMap<>();
        for (Object[] row : matchingLogRepository.countResponsesByWorkers(workerIds, RESPONSE_RESULTS, since)) {
            long[] s = stats.computeIfAbsent((Long) row[0], k -> new long[2]);
            long count = ((Number) row[2]).longValue();
            s[1] += count;
            if (row[1] == MatchingResult.ACCEPTED) s[0] += count;
        }
        return stats;
    }

    private BigDecimal calculateWorkloadScore(int ongoingJobs) {
        
        if (ongoingJobs == 0) return BigDecimal.valueOf(100);
        if (ongoingJobs == 1) return BigDecimal.valueOf(80);
        if (ongoingJobs == 2) return BigDecimal.valueOf(60);
        return BigDecimal.valueOf(40);
    }

    private MatchingLog createMatchingLog(RepairRequest request, WorkerProfile worker, WorkerScoreDTO score) {
        return MatchingLog.builder()
                .request(request)
                .worker(worker)
                .totalScore(score.getTotalScore())
                .distanceScore(score.getDistanceScore())
                .ratingScore(score.getRatingScore())
                .acceptanceRateScore(score.getAcceptanceScore())
                .workloadScore(score.getWorkloadScore())
                .result(MatchingResult.PENDING)
                .sentAt(LocalDateTime.now())
                .build();
    }

    
    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void checkMatchingTimeouts() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(MATCHING_TIMEOUT_MINUTES);
        
        List<MatchingLog> timedOut = matchingLogRepository.findByResultAndSentAtBefore(
                MatchingResult.PENDING, 
                LocalDateTime.now().minusMinutes(MATCHING_TIMEOUT_MINUTES)
        );

        for (MatchingLog log : timedOut) {
            handleTimeout(log);
        }
    }

    @Transactional
    public void handleWorkerResponse(Long matchingLogId, Long workerUserId, MatchingResult result, String note) {
        MatchingLog log = matchingLogRepository.findById(matchingLogId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy log matching"));

        
        if (!log.getWorker().getUser().getId().equals(workerUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Không có quyền thực hiện");
        }

        
        if (result != MatchingResult.ACCEPTED && result != MatchingResult.REJECTED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Kết quả phản hồi không hợp lệ");
        }

        if (log.getResult() != MatchingResult.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Yêu cầu này đã được xử lý");
        }

        
        
        RepairRequest request = repairRequestRepository.findByIdForUpdate(log.getRequest().getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));
        if (!isOpenForMatching(request)) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Yêu cầu không còn khả dụng");
        }

        log.setResult(result);
        log.setRespondedAt(LocalDateTime.now());
        log.setNote(note);
        matchingLogRepository.save(log);

        if (result == MatchingResult.ACCEPTED) {
            
            workerLoadService.assignJob(log.getWorker().getId());
            request.setWorker(log.getWorker());
            request.setStatus(RepairStatus.MATCHED);
            request.setUpdatedAt(LocalDateTime.now());
            
            
            matchingLogRepository.findByRequestIdAndResultList(request.getId(), MatchingResult.PENDING)
                    .forEach(logItem -> {
                        logItem.setResult(MatchingResult.CANCELLED_MIDWAY);
                        logItem.setRespondedAt(LocalDateTime.now());
                        logItem.setNote("Thợ khác đã chấp nhận");
                        
                        notificationService.sendMatchingClosed(logItem.getWorker().getUser().getId(), request.getId());
                    });
            matchingLogRepository.saveAll(matchingLogRepository.findByRequestIdAndResultList(request.getId(), MatchingResult.PENDING));
            
            
            notificationService.sendWorkerAccepted(request.getCustomer().getId(), request.getId(), log.getWorker().getUser().getFullName());
            
        } else {
            
            findNextWorker(request);
        }
    }


    private void findNextWorker(RepairRequest request) {
        if (!isOpenForMatching(request)) {
            return;
        }

        
        List<Long> triedWorkerIds = matchingLogRepository.findByRequestId(request.getId(), Pageable.unpaged())
                .stream()
                .map(log -> log.getWorker().getId())
                .toList();

        
        Candidates candidates = findCandidates(request);
        List<WorkerProfile> workers = candidates.workers().stream()
                .filter(w -> !triedWorkerIds.contains(w.getId()))
                .toList();

        if (!workers.isEmpty()) {
            
            List<WorkerScoreDTO> scored = calculateScores(request, workers, candidates.locations());

            Optional<WorkerScoreDTO> nextWorker = scored.stream()
                    .filter(w -> w.getDistanceKm() <= MAX_DISTANCE_KM)
                    .findFirst();

            if (nextWorker.isPresent()) {
                WorkerScoreDTO next = nextWorker.get();
                MatchingLog log = createMatchingLog(request, next.getWorker(), next);
                matchingLogRepository.save(log);
                notificationService.sendMatchingRequest(next.getWorker().getUser().getId(), request.getId(), log.getId());
                return;
            }
        }

        
        if (matchingLogRepository.countByRequestIdAndResult(request.getId(), MatchingResult.PENDING) > 0) {
            return;
        }
        request.setStatus(RepairStatus.NOT_FOUND);
        request.setUpdatedAt(LocalDateTime.now());
        repairRequestRepository.save(request);
        notificationService.sendNoWorkerFound(request.getCustomer().getId(), request.getId());
    }

    private void handleTimeout(MatchingLog log) {
        log.setResult(MatchingResult.EXPIRED);
        log.setRespondedAt(LocalDateTime.now());
        log.setNote("Thợ không phản hồi trong thời gian chờ");
        matchingLogRepository.save(log);
        notificationService.sendMatchingClosed(log.getWorker().getUser().getId(), log.getRequest().getId());

        
        repairRequestRepository.findByIdForUpdate(log.getRequest().getId())
                .ifPresent(this::findNextWorker);
    }

    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkerScoreDTO {
        private WorkerProfile worker;
        private double distanceKm;
        private int durationMinutes;
        private BigDecimal distanceScore;
        private BigDecimal ratingScore;
        private BigDecimal acceptanceScore;
        private BigDecimal workloadScore;
        private BigDecimal totalScore;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkerMatchDTO {
        private Long workerId;
        private String workerName;
        private double distanceKm;
        private int durationMinutes;
        private BigDecimal totalScore;
        private Long matchingLogId;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchingResultDTO {
        private boolean success;
        private String message;
        private List<WorkerMatchDTO> matchedWorkers;
    }
}