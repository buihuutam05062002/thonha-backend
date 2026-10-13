package com.thonha.backend.dto.response;

import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.WarrantyClaim;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarrantyClaimResponse {
    Long id;
    String status;
    Long requestId;
    String requestCode;
    String categoryName;
    String customerName;
    String customerPhone;
    String addressText;
    BigDecimal lat;
    BigDecimal lng;
    String description;
    List<String> evidenceImages;
    LocalDateTime orderCompletedAt;
    LocalDateTime warrantyExpiresAt;
    long daysLeft;
    LocalDateTime createdAt;
    LocalDateTime scheduledAt;
    String scheduleNote;
    String rejectReason;
    String resolutionType;
    String resolutionNote;
    List<String> resultImages;
    LocalDateTime completedAt;

    public static WarrantyClaimResponse from(WarrantyClaim claim) {
        RepairRequest request = claim.getRequest();
        LocalDateTime expiresAt = request.getWarrantyExpiresAt();
        long daysLeft = expiresAt == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), expiresAt.toLocalDate()));

        return WarrantyClaimResponse.builder()
                .id(claim.getId())
                .status(claim.getStatus().name())
                .requestId(request.getId())
                .requestCode(request.getRequestCode())
                .categoryName(request.getCategory().getName())
                .customerName(request.getCustomer().getFullName())
                .customerPhone(request.getCustomer().getPhoneNumber())
                .addressText(request.getAddressText())
                .lat(request.getLat())
                .lng(request.getLng())
                .description(claim.getDescription())
                .evidenceImages(new ArrayList<>(claim.getEvidenceUrls()))
                .orderCompletedAt(request.getCompletedAt())
                .warrantyExpiresAt(expiresAt)
                .daysLeft(daysLeft)
                .createdAt(claim.getCreatedAt())
                .scheduledAt(claim.getScheduledAt())
                .scheduleNote(claim.getScheduleNote())
                .rejectReason(claim.getRejectReason())
                .resolutionType(claim.getResolutionType() == null ? null : claim.getResolutionType().name())
                .resolutionNote(claim.getResolutionNote())
                .resultImages(new ArrayList<>(claim.getResultUrls()))
                .completedAt(claim.getCompletedAt())
                .build();
    }
}
