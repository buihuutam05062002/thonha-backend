package com.thonha.backend.dto.admin;

import com.thonha.backend.entity.WorkerDocument;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.entity.WorkerSpecialty;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.AvailabilityStatus;
import com.thonha.backend.enums.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;


public record WorkerApprovalResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        String provinceCity,
        String operatingArea,
        Integer experienceYears,
        ApprovalStatus approvalStatus,
        AvailabilityStatus availabilityStatus,
        BigDecimal averageRating,
        BigDecimal acceptanceRate,
        Integer ongoingJobsCount,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        Long reviewedById,
        String reviewedByName,
        String rejectReason,
        List<Specialty> specialties,
        List<Document> documents,
        Bank bankAccount,
        boolean canApprove,
        List<DocumentType> missingDocuments) {

    public record Specialty(Long id, String name) {
    }

    public record Document(Long id, DocumentType type, String typeLabel, String fileUrl, LocalDateTime createdAt) {
    }

    /** Số tài khoản được che, chỉ giữ 4 số cuối. */
    public record Bank(String bankName, String accountHolder, String accountNumberMasked) {
    }

    public record Stats(long pending, long approved, long rejected) {
    }

    /** Giấy tờ bắt buộc phải có trước khi duyệt. */
    public static final List<DocumentType> REQUIRED_DOCUMENTS =
            List.of(DocumentType.CCCD_FRONT, DocumentType.CCCD_BACK);

    public static List<DocumentType> findMissingDocuments(WorkerProfile p) {
        return REQUIRED_DOCUMENTS.stream()
                .filter(req -> p.getDocuments().stream().noneMatch(d -> d.getType() == req))
                .toList();
    }

    public static WorkerApprovalResponse from(WorkerProfile p) {
        var user = p.getUser();
        var reviewer = p.getReviewedBy();
        var bank = p.getBankAccount();
        List<DocumentType> missing = findMissingDocuments(p);

        return new WorkerApprovalResponse(
                p.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                p.getProvinceCity(),
                p.getOperatingArea(),
                p.getExperienceYears(),
                p.getApprovalStatus(),
                p.getAvailabilityStatus(),
                p.getAverageRating(),
                p.getAcceptanceRate(),
                p.getOngoingJobsCount(),
                p.getCreatedAt(),
                p.getReviewedAt(),
                reviewer == null ? null : reviewer.getId(),
                reviewer == null ? null : reviewer.getFullName(),
                p.getRejectReason(),
                p.getSpecialties().stream()
                        .map((WorkerSpecialty s) -> new Specialty(s.getServiceCategory().getId(), s.getServiceCategory().getName()))
                        .sorted(Comparator.comparing(Specialty::id))
                        .toList(),
                p.getDocuments().stream()
                        .map((WorkerDocument d) -> new Document(d.getId(), d.getType(), d.getType().getDescription(), d.getFileUrl(), d.getCreatedAt()))
                        .toList(),
                bank == null ? null : new Bank(bank.getBankName(), bank.getAccountHolder(), mask(bank.getAccountNumber())),
                missing.isEmpty() && p.getApprovalStatus() == ApprovalStatus.PENDING,
                missing
        );
    }

    private static String mask(String number) {
        if (number == null || number.length() <= 4) return number;
        return "*".repeat(number.length() - 4) + number.substring(number.length() - 4);
    }
}
