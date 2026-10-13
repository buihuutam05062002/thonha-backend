package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.worker.WorkerIncomeResponse;
import com.thonha.backend.dto.worker.WorkerOrderItemResponse;
import com.thonha.backend.dto.worker.WorkerReviewResponse;
import com.thonha.backend.dto.worker.WorkerReviewSummaryResponse;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.RepairRequestSpecifications;
import com.thonha.backend.repository.ReviewRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkerEarningsService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_INCOME_DAYS = 7;

    private final WorkerProfileRepository workerProfileRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentTransactionService paymentTransactionService;

    public WorkerEarningsService(WorkerProfileRepository workerProfileRepository,
                                 RepairRequestRepository repairRequestRepository,
                                 ReviewRepository reviewRepository,
                                 PaymentTransactionService paymentTransactionService) {
        this.workerProfileRepository = workerProfileRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.reviewRepository = reviewRepository;
        this.paymentTransactionService = paymentTransactionService;
    }

    @Transactional(readOnly = true)
    public WorkerIncomeResponse income(Long userId, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : (from != null ? from.plusDays(DEFAULT_INCOME_DAYS - 1) : LocalDate.now());
        LocalDate begin = from != null ? from : end.minusDays(DEFAULT_INCOME_DAYS - 1);
        return paymentTransactionService.incomeForWorker(workerId(userId), begin, end);
    }

    @Transactional(readOnly = true)
    public Page<WorkerOrderItemResponse> orders(Long userId, Collection<RepairStatus> statuses, String keyword,
                                                int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return repairRequestRepository
                .findAll(RepairRequestSpecifications.forWorker(workerId(userId), statuses, keyword), pageable)
                .map(WorkerOrderItemResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<WorkerReviewResponse> reviews(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return reviewRepository.findByWorkerId(workerId(userId), pageable).map(WorkerReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkerReviewSummaryResponse reviewSummary(Long userId) {
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : reviewRepository.countByRatingForWorker(workerId(userId))) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return RatingDistribution.summarize(counts);
    }

    private Long workerId(Long userId) {
        return workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND))
                .getId();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
