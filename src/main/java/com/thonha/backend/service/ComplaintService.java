package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.CreateComplaintRequest;
import com.thonha.backend.dto.request.ResolveComplaintRequest;
import com.thonha.backend.dto.response.AdminComplaintDetailResponse;
import com.thonha.backend.dto.response.AdminComplaintDetailResponse.PartyInfo;
import com.thonha.backend.dto.response.AdminComplaintDetailResponse.PaymentInfo;
import com.thonha.backend.dto.response.AdminComplaintDetailResponse.RequestInfo;
import com.thonha.backend.dto.response.ComplaintResponse;
import com.thonha.backend.entity.Complaint;
import com.thonha.backend.entity.PaymentTransaction;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.User;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.enums.ComplaintReason;
import com.thonha.backend.enums.ComplaintStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.enums.TransactionStatus;
import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.ComplaintRepository;
import com.thonha.backend.repository.ComplaintSpecifications;
import com.thonha.backend.repository.PaymentTransactionRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.UserRepository;
import com.thonha.backend.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private static final Set<RepairStatus> COMPLAINABLE_STATUSES =
            EnumSet.of(RepairStatus.MATCHED, RepairStatus.ON_THE_WAY, RepairStatus.IN_PROGRESS, RepairStatus.COMPLETED);
    private static final List<ComplaintStatus> OPEN_STATUSES =
            List.of(ComplaintStatus.PENDING, ComplaintStatus.PROCESSING);
    private static final int TEXT_MAX = 2000;
    private static final String IMAGE_FOLDER = "complaints";

    private final ComplaintRepository complaintRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CloudinaryService cloudinaryService;
    private final AttachmentValidator attachmentValidator;
    private final NotificationService notificationService;


    @Transactional
    public ComplaintResponse create(CreateComplaintRequest request, List<MultipartFile> images) {
        Long userId = currentUserProvider.requireUserId();

        RepairRequest repairRequest = repairRequestRepository.findByIdForUpdate(request.getRequestId())
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND, "Không tìm thấy yêu cầu sửa chữa"));

        boolean isCustomer = repairRequest.getCustomer().getId().equals(userId);
        boolean isWorker = repairRequest.getWorker() != null
                && repairRequest.getWorker().getUser().getId().equals(userId);
        if (!isCustomer && !isWorker) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không thuộc yêu cầu này nên không thể khiếu nại");
        }
        if (repairRequest.getWorker() == null || !COMPLAINABLE_STATUSES.contains(repairRequest.getStatus())) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY,
                    "Chỉ khiếu nại được khi đơn đã có thợ nhận và chưa bị hủy");
        }
        if (complaintRepository.existsByRequestIdAndComplainantIdAndStatusIn(
                repairRequest.getId(), userId, OPEN_STATUSES)) {
            throw new ApiException(ErrorCode.CONFLICT, "Bạn đã có khiếu nại đang được xử lý cho đơn này");
        }

        String description = request.getDescription() == null ? "" : request.getDescription().trim();
        if (description.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Vui lòng mô tả vấn đề");
        }
        if (description.length() > TEXT_MAX) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Mô tả tối đa " + TEXT_MAX + " ký tự");
        }

        List<AttachmentValidator.Validated> files = attachmentValidator.validate(images);
        for (AttachmentValidator.Validated f : files) {
            if (f.kind() != AttachmentValidator.Kind.IMAGE) {
                throw new ApiException(ErrorCode.INVALID_FILE_TYPE, "Khiếu nại chỉ nhận ảnh JPG, PNG hoặc WEBP");
            }
        }

        User complainant = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Complaint complaint = Complaint.builder()
                .request(repairRequest)
                .complainant(complainant)
                .reason(request.getReason())
                .description(description)
                .build();

        List<CloudinaryService.UploadResult> uploaded = new ArrayList<>();
        try {
            for (AttachmentValidator.Validated f : files) {
                CloudinaryService.UploadResult up = cloudinaryService.uploadAs(f.file(), IMAGE_FOLDER, "image");
                uploaded.add(up);
                complaint.addImage(up.url());
            }
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly);
            log.warn("Upload ảnh khiếu nại thất bại: {}", e.getMessage());
            throw new ApiException(ErrorCode.FILE_UPLOAD_ERROR, "Không tải được ảnh, vui lòng thử lại");
        }

        Complaint saved;
        try {
            saved = complaintRepository.saveAndFlush(complaint);
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly); // dọn ảnh mồ côi
            throw e;
        }

        for (Long adminId : userRepository.findIdsByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)) {
            notificationService.sendComplaintCreated(adminId, repairRequest.getId(), repairRequest.getRequestCode());
        }
        return ComplaintResponse.from(saved, true);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> mine(Long requestId) {
        Long userId = currentUserProvider.requireUserId();
        return complaintRepository.findByComplainant(userId).stream()
                .filter(c -> requestId == null || c.getRequest().getId().equals(requestId))
                .map(c -> ComplaintResponse.from(c, true))
                .toList();
    }


    @Transactional(readOnly = true)
    public Page<ComplaintResponse> search(ComplaintStatus status, ComplaintReason reason,
                                          String keyword, Pageable pageable) {
        return complaintRepository.findAll(ComplaintSpecifications.search(status, reason, keyword), pageable)
                .map(c -> ComplaintResponse.from(c, false));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ComplaintStatus s : ComplaintStatus.values()) {
            result.put(s.name(), 0L);
        }
        long total = 0;
        for (Object[] row : complaintRepository.countByStatus()) {
            long count = ((Number) row[1]).longValue();
            result.put(((ComplaintStatus) row[0]).name(), count);
            total += count;
        }
        result.put("ALL", total);
        return result;
    }

    @Transactional(readOnly = true)
    public AdminComplaintDetailResponse detail(Long id) {
        Complaint c = complaintRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPLAINT_NOT_FOUND));
        RepairRequest r = c.getRequest();
        WorkerProfile w = r.getWorker();
        PaymentTransaction pay = paymentTransactionRepository
                .findFirstByRequestIdAndStatus(r.getId(), TransactionStatus.SUCCESS).orElse(null);
        Long adminId = currentUserProvider.requireUserId();

        return AdminComplaintDetailResponse.builder()
                .complaint(ComplaintResponse.from(c, true))
                .handledByMe(c.getHandledBy() != null && c.getHandledBy().getId().equals(adminId))
                .request(RequestInfo.builder()
                        .id(r.getId())
                        .requestCode(r.getRequestCode())
                        .categoryName(r.getCategory().getName())
                        .status(r.getStatus().name())
                        .statusLabel(r.getStatus().getDescription())
                        .description(r.getDescription())
                        .addressText(r.getAddressText())
                        .finalPrice(r.getFinalPrice())
                        .createdAt(r.getCreatedAt())
                        .build())
                .customer(party(r.getCustomer(), null))
                .worker(w != null ? party(w.getUser(), w.getId()) : null)
                .payment(pay == null ? null : PaymentInfo.builder()
                        .amount(pay.getAmount())
                        .commissionAmount(pay.getCommissionAmount())
                        .workerAmount(pay.getWorkerAmount())
                        .paymentMethod(pay.getPaymentMethod().name())
                        .status(pay.getStatus().name())
                        .build())
                .build();
    }


    @Transactional
    public ComplaintResponse take(Long id) {
        Long adminId = currentUserProvider.requireUserId();
        Complaint c = complaintRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPLAINT_NOT_FOUND));

        if (c.getStatus() == ComplaintStatus.PROCESSING) {
            boolean mine = c.getHandledBy() != null && c.getHandledBy().getId().equals(adminId);
            throw new ApiException(ErrorCode.CONFLICT, mine
                    ? "Bạn đã nhận xử lý khiếu nại này rồi"
                    : "Khiếu nại này đã được admin khác nhận xử lý");
        }
        if (c.getStatus() != ComplaintStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Khiếu nại này đã được đóng");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        c.setStatus(ComplaintStatus.PROCESSING);
        c.setHandledBy(admin);
        Complaint saved = complaintRepository.saveAndFlush(c);

        ComplaintResponse response = ComplaintResponse.from(saved, true);
        notificationService.sendComplaintProcessing(response.getComplainantId(), response.getRequestId());
        return response;
    }

    @Transactional
    public ComplaintResponse resolve(Long id, ResolveComplaintRequest body) {
        Long adminId = currentUserProvider.requireUserId();

        if (body.getStatus() != ComplaintStatus.RESOLVED && body.getStatus() != ComplaintStatus.REJECTED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Kết quả xử lý chỉ có thể là giải quyết hoặc từ chối");
        }
        String resolution = body.getResolution() == null ? "" : body.getResolution().trim();
        if (resolution.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Vui lòng nhập nội dung kết luận");
        }
        if (resolution.length() > TEXT_MAX) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Nội dung kết luận tối đa " + TEXT_MAX + " ký tự");
        }

        Complaint c = complaintRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPLAINT_NOT_FOUND));

        if (c.getStatus() == ComplaintStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Hãy nhận xử lý khiếu nại trước khi kết luận");
        }
        if (c.getStatus() != ComplaintStatus.PROCESSING) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Khiếu nại này đã được đóng");
        }
        if (c.getHandledBy() == null || !c.getHandledBy().getId().equals(adminId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Chỉ admin đang xử lý khiếu nại này mới được kết luận");
        }

        c.setStatus(body.getStatus());
        c.setResolution(resolution);
        c.setResolvedAt(LocalDateTime.now());
        Complaint saved = complaintRepository.saveAndFlush(c);

        ComplaintResponse response = ComplaintResponse.from(saved, true);
        boolean resolved = saved.getStatus() == ComplaintStatus.RESOLVED;
        notificationService.sendComplaintClosed(response.getComplainantId(), response.getRequestId(), resolved, true);
        if (response.getAgainstId() != null) {
            notificationService.sendComplaintClosed(response.getAgainstId(), response.getRequestId(), resolved, false);
        }
        return response;
    }

    private static PartyInfo party(User user, Long workerProfileId) {
        return PartyInfo.builder()
                .userId(user.getId())
                .workerProfileId(workerProfileId)
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
