package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.AcceptWarrantyRequest;
import com.thonha.backend.dto.request.CompleteWarrantyRequest;
import com.thonha.backend.dto.request.CreateWarrantyClaimRequest;
import com.thonha.backend.dto.request.RejectWarrantyRequest;
import com.thonha.backend.dto.response.WarrantyClaimResponse;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.WarrantyClaim;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.enums.WarrantyStatus;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.WarrantyClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyService {
    private static final String IMAGE_FOLDER = "thonha/warranty";
    private static final int MAX_IMAGES = 5;
    private static final int MAX_LIST_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Set<String> TIME_SLOTS = Set.of("08:00", "10:00", "14:00", "16:00");
    private static final Set<WarrantyStatus> OPEN_STATUSES =
            EnumSet.of(WarrantyStatus.PENDING, WarrantyStatus.PROCESSING);
    private static final Map<String, Set<WarrantyStatus>> TABS = Map.of(
            "PENDING", EnumSet.of(WarrantyStatus.PENDING),
            "PROCESSING", EnumSet.of(WarrantyStatus.PROCESSING),
            "COMPLETED", EnumSet.of(WarrantyStatus.COMPLETED, WarrantyStatus.REJECTED));

    private final WarrantyClaimRepository claimRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final CloudinaryService cloudinaryService;
    private final AttachmentValidator attachmentValidator;
    private final WarrantyNotifier notifier;

    // ===================== Customer =====================
    
    @Transactional
    public WarrantyClaimResponse createClaim(Long customerId, CreateWarrantyClaimRequest data,
                                             List<MultipartFile> files) {
        RepairRequest request = repairRequestRepository.findByIdForUpdate(data.getRequestId())
                .filter(r -> r.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));

        if (request.getStatus() != RepairStatus.COMPLETED || request.getWorker() == null) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ yêu cầu đã hoàn thành mới được bảo hành");
        }
        if (request.getWarrantyExpiresAt() == null || LocalDateTime.now().isAfter(request.getWarrantyExpiresAt())) {
            throw new ApiException(ErrorCode.WARRANTY_EXPIRED);
        }
        if (claimRepository.existsByRequestIdAndStatusIn(request.getId(), OPEN_STATUSES)) {
            throw new ApiException(ErrorCode.WARRANTY_CLAIM_EXISTS);
        }

        List<AttachmentValidator.Validated> validated = validateImages(files);

        WarrantyClaim claim = WarrantyClaim.builder()
                .request(request)
                .worker(request.getWorker())
                .description(data.getDescription().trim())
                .build();
        List<CloudinaryService.UploadResult> uploaded = uploadImages(validated, claim.getEvidenceUrls());

        try {
            claimRepository.saveAndFlush(claim);
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly);
            throw e;
        }

        notifier.notifyUser(request.getWorker().getUser().getId(), "WARRANTY_CLAIM_CREATED",
                claim.getId(), request.getId(),
                "Khách gửi yêu cầu bảo hành cho đơn #" + request.getRequestCode());
        return WarrantyClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> listMine(Long customerId) {
        return claimRepository.findByRequestCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(WarrantyClaimResponse::from).toList();
    }

    // ===================== Worker =====================
    
    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> listForWorker(Long workerUserId, String tab, String keyword) {
        Set<WarrantyStatus> statuses =
                TABS.getOrDefault(tab == null ? "PENDING" : tab.toUpperCase(), TABS.get("PENDING"));
        String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
        return claimRepository.search(workerUserId, statuses, pattern).stream()
                .limit(MAX_LIST_SIZE)
                .map(WarrantyClaimResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> counts(Long workerUserId) {
        return Map.of(
                "PENDING", claimRepository.countByWorkerUserIdAndStatusIn(workerUserId, TABS.get("PENDING")),
                "PROCESSING", claimRepository.countByWorkerUserIdAndStatusIn(workerUserId, TABS.get("PROCESSING")),
                "COMPLETED", claimRepository.countByWorkerUserIdAndStatusIn(workerUserId, TABS.get("COMPLETED")));
    }

    @Transactional(readOnly = true)
    public WarrantyClaimResponse detail(Long workerUserId, Long claimId) {
        return claimRepository.findByIdAndWorkerUserId(claimId, workerUserId)
                .map(WarrantyClaimResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.WARRANTY_NOT_FOUND));
    }

    @Transactional
    public WarrantyClaimResponse reject(Long workerUserId, Long claimId, RejectWarrantyRequest data) {
        WarrantyClaim claim = lockOwnClaim(workerUserId, claimId);
        requireStatus(claim, WarrantyStatus.PENDING, "Chỉ phiếu đang chờ tiếp nhận mới có thể từ chối");

        claim.setStatus(WarrantyStatus.REJECTED);
        claim.setRejectReason(data.getReason().trim());
        claim.setRespondedAt(LocalDateTime.now());

        notifier.notifyUser(customerIdOf(claim), "WARRANTY_REJECTED", claim.getId(), claim.getRequest().getId(),
                "Thợ đã từ chối yêu cầu bảo hành: " + claim.getRejectReason());
        return WarrantyClaimResponse.from(claim);
    }

    @Transactional
    public WarrantyClaimResponse accept(Long workerUserId, Long claimId, AcceptWarrantyRequest data) {
        WarrantyClaim claim = lockOwnClaim(workerUserId, claimId);
        requireStatus(claim, WarrantyStatus.PENDING, "Chỉ phiếu đang chờ tiếp nhận mới có thể lên lịch");

        if (!TIME_SLOTS.contains(data.getAppointmentTime())) {
            throw new ApiException(ErrorCode.INVALID_SCHEDULE, "Khung giờ hẹn không hợp lệ");
        }
        LocalDateTime scheduledAt = LocalDateTime.of(data.getAppointmentDate(),
                LocalTime.parse(data.getAppointmentTime()));
        if (scheduledAt.plusHours(2).isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.INVALID_SCHEDULE, "Khung giờ hẹn đã qua");
        }

        claim.setStatus(WarrantyStatus.PROCESSING);
        claim.setScheduledAt(scheduledAt);
        claim.setScheduleNote(data.getNote() == null || data.getNote().isBlank() ? null : data.getNote().trim());
        claim.setRespondedAt(LocalDateTime.now());

        notifier.notifyUser(customerIdOf(claim), "WARRANTY_SCHEDULED", claim.getId(), claim.getRequest().getId(),
                "Thợ đã tiếp nhận và hẹn lịch bảo hành lúc " + scheduledAt.format(DATE_TIME));
        return WarrantyClaimResponse.from(claim);
    }

    @Transactional
    public WarrantyClaimResponse complete(Long workerUserId, Long claimId, CompleteWarrantyRequest data,
                                          List<MultipartFile> files) {
        WarrantyClaim claim = lockOwnClaim(workerUserId, claimId);
        requireStatus(claim, WarrantyStatus.PROCESSING, "Chỉ phiếu đang xử lý mới có thể nghiệm thu");

        List<AttachmentValidator.Validated> validated = validateImages(files);
        List<CloudinaryService.UploadResult> uploaded = uploadImages(validated, claim.getResultUrls());

        claim.setStatus(WarrantyStatus.COMPLETED);
        claim.setResolutionType(data.getReasonType());
        claim.setResolutionNote(data.getResolutionText().trim());
        claim.setCompletedAt(LocalDateTime.now());

        try {
            claimRepository.saveAndFlush(claim);
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly);
            throw e;
        }

        notifier.notifyUser(customerIdOf(claim), "WARRANTY_COMPLETED", claim.getId(), claim.getRequest().getId(),
                "Thợ đã hoàn tất bảo hành cho đơn #" + claim.getRequest().getRequestCode());
        return WarrantyClaimResponse.from(claim);
    }

    private WarrantyClaim lockOwnClaim(Long workerUserId, Long claimId) {
        WarrantyClaim claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new ApiException(ErrorCode.WARRANTY_NOT_FOUND));
        if (!claim.getWorker().getUser().getId().equals(workerUserId)) {
            throw new ApiException(ErrorCode.WARRANTY_NOT_FOUND); // do not reveal other workers' claims
        }
        return claim;
    }

    private void requireStatus(WarrantyClaim claim, WarrantyStatus expected, String message) {
        if (claim.getStatus() != expected) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, message);
        }
    }

    private Long customerIdOf(WarrantyClaim claim) {
        return claim.getRequest().getCustomer().getId();
    }

    private List<AttachmentValidator.Validated> validateImages(List<MultipartFile> files) {
        List<AttachmentValidator.Validated> validated = attachmentValidator.validate(files);
        if (validated.size() > MAX_IMAGES) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tối đa " + MAX_IMAGES + " ảnh");
        }
        for (AttachmentValidator.Validated f : validated) {
            if (f.kind() != AttachmentValidator.Kind.IMAGE) {
                throw new ApiException(ErrorCode.INVALID_FILE_TYPE, "Chỉ nhận ảnh JPG, PNG hoặc WEBP");
            }
        }
        return validated;
    }

    private List<CloudinaryService.UploadResult> uploadImages(List<AttachmentValidator.Validated> validated,
                                                              List<String> targetUrls) {
        List<CloudinaryService.UploadResult> uploaded = new ArrayList<>();
        try {
            for (AttachmentValidator.Validated f : validated) {
                CloudinaryService.UploadResult up = cloudinaryService.uploadAs(f.file(), IMAGE_FOLDER, "image");
                uploaded.add(up);
                targetUrls.add(up.url());
            }
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly);
            log.warn("Warranty image upload failed: {}", e.getMessage());
            throw new ApiException(ErrorCode.FILE_UPLOAD_ERROR, "Không tải được ảnh, vui lòng thử lại");
        }
        return uploaded;
    }
}
