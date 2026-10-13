package com.thonha.backend.dto.response;

import com.thonha.backend.entity.CommissionPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommissionPolicyResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal ratePercent,
        boolean active,
        LocalDateTime updatedAt) {

    public static CommissionPolicyResponse from(CommissionPolicy p) {
        return new CommissionPolicyResponse(
                p.getId(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? "Mặc định (toàn hệ thống)" : p.getCategory().getName(),
                p.getRatePercent(), p.isActive(), p.getUpdatedAt());
    }


    public record Overview(BigDecimal defaultRatePercent, boolean defaultFromConfig,
                           List<CommissionPolicyResponse> categoryPolicies) {
    }
}
