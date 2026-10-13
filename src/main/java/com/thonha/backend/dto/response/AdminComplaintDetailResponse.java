package com.thonha.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminComplaintDetailResponse {
    ComplaintResponse complaint;
    boolean handledByMe;
    RequestInfo request;
    PartyInfo customer;
    PartyInfo worker;
    PaymentInfo payment;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RequestInfo {
        Long id;
        String requestCode;
        String categoryName;
        String status;
        String statusLabel;
        String description;
        String addressText;
        BigDecimal finalPrice;
        LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PartyInfo {
        Long userId;
        Long workerProfileId;
        String fullName;
        String phoneNumber;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PaymentInfo {
        BigDecimal amount;
        BigDecimal commissionAmount;
        BigDecimal workerAmount;
        String paymentMethod;
        String status;
    }
}
