package com.thonha.backend.dto.worker;

import com.thonha.backend.entity.RepairRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkerOrderItemResponse(
        Long id,
        String requestCode,
        String customerName,
        String categoryName,
        String addressText,
        String status,
        String statusLabel,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static WorkerOrderItemResponse from(RepairRequest r) {
        return new WorkerOrderItemResponse(
                r.getId(), r.getRequestCode(),
                r.getCustomer() == null ? null : r.getCustomer().getFullName(),
                r.getCategory() == null ? null : r.getCategory().getName(),
                r.getAddressText(),
                r.getStatus().name(), r.getStatus().getDescription(),
                r.getFinalPrice(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
