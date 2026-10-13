package com.thonha.backend.dto.response;

import java.math.BigDecimal;

public record CommissionSummaryResponse(
        long orders,
        BigDecimal grossAmount,
        BigDecimal commissionAmount,
        BigDecimal workerAmount) {
}
