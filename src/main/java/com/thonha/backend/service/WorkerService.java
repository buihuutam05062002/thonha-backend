package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.admin.WorkerApprovalResponse;
import com.thonha.backend.dto.request.AvailabilityRequest;
import com.thonha.backend.dto.request.RegisterWorkerProfileRequest;
import com.thonha.backend.dto.response.WorkerProfileResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.*;
import com.thonha.backend.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkerService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    WorkerProfileRepository workerProfileRepository;
    ServiceCategoryRepository serviceCategoryRepository;
    CloudinaryService cloudinaryService;
    WorkerLocationService workerLocationService;
    RepairRequestRepository repairRequestRepository;
    NotificationService notificationService;

    public WorkerService(UserRepository userRepository,
                         RoleRepository roleRepository,
                         WorkerProfileRepository workerProfileRepository,
                         ServiceCategoryRepository serviceCategoryRepository,
                         CloudinaryService cloudinaryService,
                         WorkerLocationService workerLocationService,
                         RepairRequestRepository repairRequestRepository,
                         NotificationService notificationService) {
        this.workerLocationService = workerLocationService;
        this.repairRequestRepository = repairRequestRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public WorkerProfileResponse register(Long userId, RegisterWorkerProfileRequest request,
                                          MultipartFile cccdFront, MultipartFile cccdBack,
                                          List<MultipartFile> certificates, List<MultipartFile> degrees) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (workerProfileRepository.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.WORKER_PROFILE_EXISTS);
        }
        if (cccdFront == null || cccdFront.isEmpty() || cccdBack == null || cccdBack.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "CCCD mặt trước và mặt sau là bắt buộc");
        }

        Set<Long> categoryIds = request.getCategoryIds().stream().collect(Collectors.toSet());
        List<ServiceCategory> categories = serviceCategoryRepository.findByIdInAndStatus(categoryIds, CategoryStatus.ACTIVE);
        if (categories.size() != categoryIds.size()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều chuyên môn không hợp lệ hoặc không hoạt động");
        }

        WorkerProfile profile = WorkerProfile.builder()
                .user(userRepository.getReferenceById(userId))
                .provinceCity(request.getProvinceCity().trim())
                .operatingArea(request.getOperatingArea().trim())
                .experienceYears(request.getExperienceYears())
                .build();

        categories.forEach(profile::addSpecialty);

        String folder = "nhatho/workers/" + userId;
        profile.addDocument(DocumentType.CCCD_FRONT, cloudinaryService.upload(cccdFront, folder));
        profile.addDocument(DocumentType.CCCD_BACK, cloudinaryService.upload(cccdBack, folder));

        if (certificates != null) {
            for (MultipartFile file : certificates) {
                if (file != null && !file.isEmpty()) {
                    profile.addDocument(DocumentType.CERTIFICATE, cloudinaryService.upload(file, folder));
                }
            }
        }
        if (degrees != null) {
            for (MultipartFile file : degrees) {
                if (file != null && !file.isEmpty()) {
                    profile.addDocument(DocumentType.DEGREE, cloudinaryService.upload(file, folder));
                }
            }
        }

        workerProfileRepository.save(profile);

        if (!user.hasRole(Role.WORKER)) {
            user.getRoles().add(getRole(Role.WORKER));
        }

        return WorkerProfileResponse.from(workerProfileRepository.save(profile));
    }


    @Transactional
    public WorkerProfileResponse resubmit(Long userId, RegisterWorkerProfileRequest request,
                                          MultipartFile cccdFront, MultipartFile cccdBack,
                                          List<MultipartFile> certificates, List<MultipartFile> degrees) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        if (profile.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ hồ sơ đã bị từ chối mới được nộp lại");
        }

        Set<Long> categoryIds = request.getCategoryIds().stream().collect(Collectors.toSet());
        List<ServiceCategory> categories = serviceCategoryRepository.findByIdInAndStatus(categoryIds, CategoryStatus.ACTIVE);
        if (categories.size() != categoryIds.size()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều chuyên môn không hợp lệ hoặc không hoạt động");
        }

        profile.setProvinceCity(request.getProvinceCity().trim());
        profile.setOperatingArea(request.getOperatingArea().trim());
        profile.setExperienceYears(request.getExperienceYears());

        profile.getSpecialties().removeIf(s -> !categoryIds.contains(s.getServiceCategory().getId()));
        categories.forEach(profile::addSpecialty);

        String folder = "nhatho/workers/" + userId;
        replaceDocuments(profile, DocumentType.CCCD_FRONT, singleton(cccdFront), folder);
        replaceDocuments(profile, DocumentType.CCCD_BACK, singleton(cccdBack), folder);
        replaceDocuments(profile, DocumentType.CERTIFICATE, certificates, folder);
        replaceDocuments(profile, DocumentType.DEGREE, degrees, folder);

        List<DocumentType> missing = WorkerApprovalResponse.findMissingDocuments(profile);
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.MISSING_REQUIRED_DOCUMENTS, "Hồ sơ còn thiếu giấy tờ bắt buộc");
        }

        profile.setApprovalStatus(ApprovalStatus.PENDING);
        profile.setRejectReason(null);
        profile.setReviewedBy(null);
        profile.setReviewedAt(null);
        return WorkerProfileResponse.from(workerProfileRepository.save(profile));
    }

    private static List<MultipartFile> singleton(MultipartFile file) {
        return file == null ? null : List.of(file);
    }

    private void replaceDocuments(WorkerProfile profile, DocumentType type, List<MultipartFile> files, String folder) {
        if (files == null) return;
        List<MultipartFile> valid = files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (valid.isEmpty()) return;
        profile.getDocuments().removeIf(d -> d.getType() == type);
        valid.forEach(f -> profile.addDocument(type, cloudinaryService.upload(f, folder)));
    }

    @Transactional(readOnly = true)
    public WorkerProfileResponse getProfile(Long userId) {
        return workerProfileRepository.findByUserId(userId)
                .map(WorkerProfileResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
    }

    @Transactional
    public WorkerProfileResponse updateAvailability(Long userId, AvailabilityRequest request) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));

        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(ErrorCode.WORKER_NOT_APPROVED);
        }
        if (profile.getOngoingJobsCount() > 0) {
            throw new ApiException(ErrorCode.WORKER_HAS_ONGOING_JOBS);
        }

        profile.setAvailabilityStatus(request.isAvailable() ? AvailabilityStatus.READY : AvailabilityStatus.OFFLINE);
        WorkerProfile saved = workerProfileRepository.save(profile);
        if (!request.isAvailable()) {
            try {
                workerLocationService.remove(saved.getId());
            } catch (RuntimeException e) {
                log.warn("Không xóa được vị trí thợ {} khỏi Redis: {}", saved.getId(), e.getMessage());
            }
        }
        return WorkerProfileResponse.from(saved);
    }


    @Transactional(readOnly = true)
    public void updateLocation(Long userId, double lat, double lng) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(ErrorCode.WORKER_NOT_APPROVED);
        }
        if (profile.getAvailabilityStatus() == AvailabilityStatus.READY) {
            workerLocationService.update(profile.getId(), lat, lng);
        } else {
            workerLocationService.remove(profile.getId());
        }

        List<RepairRequest> onTheWay = repairRequestRepository.findByWorkerAndStatusIn(
                profile.getId(), List.of(RepairStatus.ON_THE_WAY));
        for (RepairRequest r : onTheWay) {
            workerLocationService.saveTracking(r.getId(), lat, lng);
            notificationService.sendWorkerLocation(r.getCustomer().getId(), r.getId(), lat, lng);
        }
    }

    private Role getRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role not configured: " + name));
    }
}