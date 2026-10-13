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

    public static QuotationResponse from(Quotation q, boolean actionable) {
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
                .build();
    }
}
