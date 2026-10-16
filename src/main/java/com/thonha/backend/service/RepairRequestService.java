package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateRepairRequest;
import com.thonha.backend.dto.response.RepairRequestResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.CategoryStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.*;
import com.thonha.backend.service.MatchingService.MatchingResultDTO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepairRequestService {
    RepairRequestRepository repairRequestRepository;
    UserRepository userRepository;
    ServiceCategoryRepository serviceCategoryRepository;
    AddressRepository addressRepository;
    CloudinaryService cloudinaryService;
    MatchingService matchingService;
    NotificationService notificationService;
    WorkerProfileRepository workerProfileRepository;
    MatchingLogRepository matchingLogRepository;
    AttachmentValidator attachmentValidator;
    WorkerLoadService workerLoadService;
    TransactionTemplate transactionTemplate;
    GoongService goongService;
    WorkerLocationService workerLocationService;
    QuotationRepository quotationRepository;
    PaymentTransactionService paymentTransactionService;



    public ApiResponse<RepairRequestResponse> create(Long userId, CreateRepairRequest request, List<MultipartFile> files) {
        List<AttachmentValidator.Validated> attachments = attachmentValidator.validate(files);

        if (request.getDescription().trim().length() < 10) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Mô tả cần ít nhất 10 ký tự");
        }

        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        ServiceCategory category = serviceCategoryRepository.findByIdAndStatus(request.getCategoryId(), CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy danh mục dịch vụ"));

        Address address = request.getAddressId() == null ? null :
                addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy địa chỉ"));


        BigDecimal latTmp = address != null && address.getLat() != null ? address.getLat()
                : request.getLat() != null ? BigDecimal.valueOf(request.getLat()) : null;
        BigDecimal lngTmp = address != null && address.getLng() != null ? address.getLng()
                : request.getLng() != null ? BigDecimal.valueOf(request.getLng()) : null;
        if (latTmp == null || lngTmp == null) {
            String text = address != null ? address.getFullAddress() : request.getAddressText();
            GoongService.Coordinate c = goongService.geocode(text);
            if (c != null) {
                latTmp = BigDecimal.valueOf(c.lat);
                lngTmp = BigDecimal.valueOf(c.lng);
            }
        }
        if (latTmp == null || lngTmp == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Chưa xác định được vị trí từ địa chỉ này. Vui lòng nhập rõ hơn, chọn từ gợi ý hoặc dùng vị trí hiện tại");
        }
        final BigDecimal lat = latTmp;
        final BigDecimal lng = lngTmp;

        List<CloudinaryService.UploadResult> uploaded = new ArrayList<>();
        try {
            for (AttachmentValidator.Validated v : attachments) {
                uploaded.add(cloudinaryService.uploadAs(v.file(), "nhatho/requests/" + userId,
                        v.kind() == AttachmentValidator.Kind.VIDEO ? "video" : "image"));
            }
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly);
            throw e;
        }
        Long requestId;
        try {
            requestId = transactionTemplate.execute(status -> {
                RepairRequest entity = new RepairRequest();
                entity.setCustomer(userRepository.getReferenceById(userId));
                entity.setCategory(serviceCategoryRepository.getReferenceById(category.getId()));
                entity.setAddress(address == null ? null : addressRepository.getReferenceById(address.getId()));
                entity.setDescription(request.getDescription().trim());
                entity.setPriorityLevel(request.getPriorityLevel());
                entity.setAddressText(request.getAddressText().trim());
                entity.setLat(lat);
                entity.setLng(lng);
                entity.setStatus(RepairStatus.PENDING_MATCH);
                entity.setRequestCode("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));

                RepairRequest saved = repairRequestRepository.saveAndFlush(entity);
                saved.setRequestCode(String.format("VT-%04d", saved.getId()));

                int sortOrder = 1;
                for (int i = 0; i < uploaded.size(); i++) {
                    RequestAttachment attachment = new RequestAttachment();
                    attachment.setRequest(saved);
                    attachment.setType(attachments.get(i).kind() == AttachmentValidator.Kind.VIDEO ? "VIDEO" : "IMAGE");
                    attachment.setUrl(uploaded.get(i).url());
                    attachment.setSortOrder(sortOrder++);
                    saved.getAttachments().add(attachment);
                }
                repairRequestRepository.save(saved);
                return saved.getId();
            });
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly);
            throw e;
        }


        try {
            matchingService.findAndMatchWorkers(requestId);
        } catch (Exception e) {
            log.warn("Ghép thợ tự động thất bại cho yêu cầu {}: {}", requestId, e.getMessage());
        }

        RepairRequestResponse response = transactionTemplate.execute(status ->
                RepairRequestResponse.from(repairRequestRepository.findById(requestId).orElseThrow()));
        return ApiResponse.success(response, "Tạo yêu cầu sửa chữa thành công");
    }


    @Transactional(readOnly = true)
    public void assertOwner(Long requestId, Long userId) {
        repairRequestRepository.findByIdAndCustomerId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));
    }

    @Transactional
    public ApiResponse<RepairRequestResponse> assignWorker(Long requestId, Long workerId) {
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));

        WorkerProfile worker = workerProfileRepository.findById(workerId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.WORKER_NOT_FOUND));

        if (request.getStatus() != RepairStatus.PENDING_MATCH && request.getStatus() != RepairStatus.MATCHING) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.INVALID_STATUS_TRANSITION,
                    "Yêu cầu không ở trạng thái chờ ghép thợ");
        }

        workerLoadService.assignJob(worker.getId());
        request.setWorker(worker);
        request.setStatus(RepairStatus.MATCHED);
        request.setUpdatedAt(LocalDateTime.now());
        repairRequestRepository.save(request);

        notificationService.sendWorkerAccepted(request.getCustomer().getId(), request.getId(), worker.getUser().getFullName());

        return ApiResponse.success(RepairRequestResponse.from(request), "Ghép thợ thành công");
    }

    @Transactional
    public ApiResponse<RepairRequestResponse> cancelRequest(Long userId, Long requestId) {
        RepairRequest request = repairRequestRepository.findByIdForUpdate(requestId)
                .filter(r -> r.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));

        if (request.getStatus() == RepairStatus.COMPLETED || request.getStatus() == RepairStatus.CANCELLED) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể hủy yêu cầu ở trạng thái này");
        }

        // Đã chuyển khoản vào tài khoản nền tảng thì không tự hủy được (tránh mất tiền khách / lệch ví thợ)
        if (paymentTransactionService.hasSuccessfulPayment(request.getId())) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.INVALID_REQUEST,
                    "Yêu cầu đã được thanh toán online nên không thể hủy. Vui lòng gửi khiếu nại để được hỗ trợ hoàn tiền");
        }
        paymentTransactionService.cancelPendingOnline(request.getId());

        boolean holdsWorkerSlot = request.getWorker() != null && (request.getStatus() == RepairStatus.MATCHED
                || request.getStatus() == RepairStatus.ON_THE_WAY || request.getStatus() == RepairStatus.IN_PROGRESS);

        request.setStatus(RepairStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());
        repairRequestRepository.save(request);
        workerLocationService.clearTracking(request.getId());

        if (holdsWorkerSlot) {
            workerLoadService.releaseJob(request.getWorker().getId());
        }

        if (request.getWorker() != null) {
            notificationService.sendRequestCancelled(request.getWorker().getUser().getId(), request.getId());
        }

        List<MatchingLog> pendingLogs = matchingLogRepository.findByRequestIdAndResultList(
                request.getId(), com.thonha.backend.enums.MatchingResult.PENDING);
        for (MatchingLog l : pendingLogs) {
            l.setResult(com.thonha.backend.enums.MatchingResult.CANCELLED_MIDWAY);
            l.setRespondedAt(LocalDateTime.now());
            l.setNote("Khách đã hủy yêu cầu");
            notificationService.sendMatchingClosed(l.getWorker().getUser().getId(), request.getId());
        }
        matchingLogRepository.saveAll(pendingLogs);

        return ApiResponse.success(RepairRequestResponse.from(request), "Hủy yêu cầu thành công");
    }

    private static final Map<RepairStatus, RepairStatus> NEXT_STATUS_BY_WORKER = Map.of(
            RepairStatus.MATCHED, RepairStatus.ON_THE_WAY,
            RepairStatus.ON_THE_WAY, RepairStatus.IN_PROGRESS,
            RepairStatus.IN_PROGRESS, RepairStatus.COMPLETED);


    @Transactional
    public ApiResponse<RepairRequestResponse> updateStatusByWorker(Long workerUserId, Long requestId, RepairStatus target,
                                                                   BigDecimal finalPrice) {
        RepairRequest request = repairRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));

        if (request.getWorker() == null || !request.getWorker().getUser().getId().equals(workerUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không phải thợ phụ trách yêu cầu này");
        }
        if (NEXT_STATUS_BY_WORKER.get(request.getStatus()) != target) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể chuyển từ \"" + request.getStatus().getDescription() + "\" sang \"" + target.getDescription() + "\"");
        }

        request.setStatus(target);
        if (target != RepairStatus.ON_THE_WAY) {
            workerLocationService.clearTracking(request.getId());
        }
        if (target == RepairStatus.COMPLETED) {
            // Khách đã thanh toán online: giá chốt chính là số tiền đã trả, thợ không được ghi đè
            if (!paymentTransactionService.hasSuccessfulPayment(request.getId())) {
                if (finalPrice != null) {
                    request.setFinalPrice(finalPrice);
                } else {
                    quotationRepository.findFirstByRequestIdAndStatusOrderByIdDesc(
                                    request.getId(), com.thonha.backend.enums.QuotationStatus.ACCEPTED)
                            .ifPresent(q -> request.setFinalPrice(q.getTotalAmount()));
                }
            }

            paymentTransactionService.settleCompletedRequest(request);
            stampWarranty(request, LocalDateTime.now());
        }
        request.setUpdatedAt(LocalDateTime.now());
        repairRequestRepository.save(request);

        Long customerUserId = request.getCustomer().getId();
        switch (target) {
            case ON_THE_WAY -> notificationService.sendWorkerOnTheWay(customerUserId, request.getId());
            case IN_PROGRESS -> notificationService.sendWorkerArrived(customerUserId, request.getId());
            case COMPLETED -> {
                workerLoadService.releaseJob(request.getWorker().getId());
                notificationService.sendJobCompleted(customerUserId, request.getId());
            }
            default -> {
            }
        }
        return ApiResponse.success(RepairRequestResponse.from(request), "Cập nhật trạng thái thành công");
    }

    private void stampWarranty(RepairRequest request, LocalDateTime completedAt) {
        request.setCompletedAt(completedAt);
        LocalDateTime latest = null;
        for (Quotation q : quotationRepository.findByRequestIdAndStatus(
                request.getId(), com.thonha.backend.enums.QuotationStatus.ACCEPTED)) {
            int days = q.getWarrantyDays() == null ? 0 : q.getWarrantyDays();
            LocalDateTime expires = days > 0 ? completedAt.plusDays(days) : null;
            q.setWarrantyExpiresAt(expires);
            if (expires != null && (latest == null || expires.isAfter(latest))) {
                latest = expires;
            }
        }
        request.setWarrantyExpiresAt(latest);
    }
    
    @Transactional(readOnly = true)
    public ApiResponse<com.thonha.backend.dto.response.WorkerLocationResponse> getWorkerLocation(Long customerUserId, Long requestId) {
        RepairRequest request = repairRequestRepository.findByIdAndCustomerId(requestId, customerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu sửa chữa"));
        if (request.getStatus() != RepairStatus.ON_THE_WAY) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(workerLocationService.getTracking(requestId)
                .map(t -> new com.thonha.backend.dto.response.WorkerLocationResponse(t.lat(), t.lng(), t.updatedAt()))
                .orElse(null));
    }


    @Transactional(readOnly = true)
    public ApiResponse<RepairRequestResponse> getDetailForWorker(Long workerUserId, Long requestId) {
        RepairRequest request = repairRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        if (request.getWorker() == null || !request.getWorker().getUser().getId().equals(workerUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không phải thợ phụ trách yêu cầu này");
        }
        return ApiResponse.success(RepairRequestResponse.from(request));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<RepairRequestResponse>> getMyRequests(Long userId) {
        List<RepairRequestResponse> requests = repairRequestRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(RepairRequestResponse::from)
                .toList();
        return ApiResponse.success(requests);
    }


    @Transactional(readOnly = true)
    public ApiResponse<org.springframework.data.domain.Page<RepairRequestResponse>> searchMyRequests(
            Long userId, java.util.Collection<com.thonha.backend.enums.RepairStatus> statuses, String keyword, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
        return ApiResponse.success(repairRequestRepository
                .findAll(com.thonha.backend.repository.RepairRequestSpecifications.forCustomer(userId, statuses, keyword), pageable)
                .map(RepairRequestResponse::from));
    }

    @Transactional(readOnly = true)
    public ApiResponse<RepairRequestResponse> getDetail(Long id, Long userId) {
        RepairRequest request = repairRequestRepository.findByIdAndCustomerId(id, userId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu sửa chữa"));
        return ApiResponse.success(RepairRequestResponse.from(request));
    }

    @Transactional(readOnly = true)
    public ApiResponse<org.springframework.data.domain.Page<RepairRequestResponse>> getAllRequests(com.thonha.backend.enums.RepairStatus status, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<RepairRequest> page = (status != null)
                ? repairRequestRepository.findByStatus(status, pageable)
                : repairRequestRepository.findAll(pageable);
        return ApiResponse.success(page.map(RepairRequestResponse::from));
    }
}