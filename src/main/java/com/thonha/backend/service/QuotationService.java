package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.CreateQuotationRequest;
import com.thonha.backend.dto.response.QuotationResponse;
import com.thonha.backend.entity.Quotation;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.QuotationStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.enums.WarrantyStatus;
import com.thonha.backend.repository.QuotationRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.WarrantyClaimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


@Service
public class QuotationService {

    private static final Set<RepairStatus> ACTIVE = EnumSet.of(
            RepairStatus.MATCHED, RepairStatus.ON_THE_WAY, RepairStatus.IN_PROGRESS);
    
    private static final Set<WarrantyStatus> OPEN_CLAIMS =
            EnumSet.of(WarrantyStatus.PENDING, WarrantyStatus.PROCESSING);
    
    private final QuotationRepository quotationRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final NotificationService notificationService;
    private final WarrantyClaimRepository warrantyClaimRepository;

    public QuotationService(QuotationRepository quotationRepository,
                            RepairRequestRepository repairRequestRepository,
                            NotificationService notificationService, WarrantyClaimRepository warrantyClaimRepository) {
        this.quotationRepository = quotationRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.notificationService = notificationService;
        this.warrantyClaimRepository = warrantyClaimRepository;
    }

    @Transactional
    public QuotationResponse create(Long workerUserId, CreateQuotationRequest body) {
        // khóa dòng yêu cầu: tránh gửi báo giá giữa lúc khách hủy / thợ hoàn thành
        RepairRequest request = repairRequestRepository.findByIdForUpdate(body.getRequestId())
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        if (request.getWorker() == null || !request.getWorker().getUser().getId().equals(workerUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không phải thợ phụ trách yêu cầu này");
        }
        if (!ACTIVE.contains(request.getStatus())) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY, "Yêu cầu đã kết thúc, không thể gửi báo giá");
        }

        Quotation q = quotationRepository.saveAndFlush(Quotation.builder()
                .request(request)
                .worker(request.getWorker())
                .totalAmount(body.getTotalAmount())
                .note(blankToNull(body.getNote()))
                .warrantyDays(body.getWarrantyDays() == null ? 0 : body.getWarrantyDays())
                .build());

        String workerName = request.getWorker().getUser().getFullName();
        Long customerId = request.getCustomer().getId();
        Long requestId = request.getId();
        QuotationResponse response = QuotationResponse.from(q, true);


        notificationService.sendNewQuotation(request.getCustomer().getId(), request.getId(),
                request.getWorker().getUser().getFullName());
        return QuotationResponse.from(q, true);
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> listByRequest(Long userId, Long requestId) {
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        boolean isCustomer = userId.equals(request.getCustomer().getId());
        boolean isWorker = request.getWorker() != null && userId.equals(request.getWorker().getUser().getId());
        if (!isCustomer && !isWorker) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem báo giá của yêu cầu này");
        }
        boolean active = ACTIVE.contains(request.getStatus());
        boolean completed = request.getStatus() == RepairStatus.COMPLETED;
        LocalDateTime now = LocalDateTime.now();

        return quotationRepository.findByRequestWithWorker(requestId).stream()
                .map(q -> {
                    boolean actionable = isCustomer && active && q.getStatus() == QuotationStatus.PENDING;
                    boolean used = q.getStatus() == QuotationStatus.ACCEPTED
                            && warrantyClaimRepository.existsByQuotationId(q.getId());
                    boolean claimable = isCustomer && completed
                            && q.getStatus() == QuotationStatus.ACCEPTED
                            && q.getWarrantyExpiresAt() != null
                            && now.isBefore(q.getWarrantyExpiresAt())
                            && !used;
                    return QuotationResponse.from(q, actionable, claimable, used);
                })
                .toList();
    }

    @Transactional
    public QuotationResponse respond(Long customerUserId, Long quotationId, boolean accept) {
        Long requestId = quotationRepository.findRequestId(quotationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy báo giá"));
        RepairRequest request = repairRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        if (!request.getCustomer().getId().equals(customerUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không phải chủ của yêu cầu này");
        }
        if (!ACTIVE.contains(request.getStatus())) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY, "Yêu cầu đã kết thúc, không thể phản hồi báo giá");
        }
        Quotation fresh = quotationRepository.findByIdWithDetails(quotationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy báo giá"));
        if (fresh.getStatus() != QuotationStatus.PENDING) {
            throw new ApiException(ErrorCode.CONFLICT, "Báo giá này không còn ở trạng thái chờ phản hồi");
        }

        fresh.setStatus(accept ? QuotationStatus.ACCEPTED : QuotationStatus.REJECTED);
        quotationRepository.saveAndFlush(fresh);

        notificationService.sendQuotationResponded(request.getWorker().getUser().getId(), request.getId(), accept);
        return QuotationResponse.from(fresh, false);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
