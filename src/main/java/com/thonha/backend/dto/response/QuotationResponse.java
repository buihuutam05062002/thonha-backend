package com.thonha.backend.dto.response;

import com.thonha.backend.entity.Quotation;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class QuotationResponse {
    Long id;
    Long requestId;
    Long workerId;
    String workerName;
    BigDecimal totalAmount;
    String note;
    String status;
    String statusDescription;
    boolean actionable;
    LocalDateTime createdAt;
    Integer warrantyDays;
    String warrantyLabel;
    LocalDateTime warrantyExpiresAt;
    boolean warrantyActive;
    boolean claimable;
    boolean warrantyUsed;

    public static String labelOf(Integer days) {
        return days == null || days <= 0 ? "Không bảo hành" : days + " ngày";
    }

    public static QuotationResponse from(Quotation q, boolean actionable) {
        return from(q, actionable, false);
    }

    public static QuotationResponse from(Quotation q, boolean actionable, boolean claimable) {
        return from(q, actionable, claimable, false);
    }

    public static QuotationResponse from(Quotation q, boolean actionable, boolean claimable, boolean warrantyUsed) {
        LocalDateTime expires = q.getWarrantyExpiresAt();
        int days = q.getWarrantyDays() == null ? 0 : q.getWarrantyDays();
        return QuotationResponse.builder()
                .id(q.getId())
                .requestId(q.getRequest().getId())
                .workerId(q.getWorker().getId())
                .workerName(q.getWorker().getUser().getFullName())
                .totalAmount(q.getTotalAmount())
                .note(q.getNote())
                .status(q.getStatus().name())
                .statusDescription(q.getStatus().getDescription())
                .actionable(actionable)
                .createdAt(q.getCreatedAt())
                .warrantyDays(days)
                .warrantyLabel(labelOf(days))
                .warrantyExpiresAt(expires)
                .warrantyActive(expires != null && LocalDateTime.now().isBefore(expires))
                .claimable(claimable)
                .warrantyUsed(warrantyUsed)
                .build();
    }
}
