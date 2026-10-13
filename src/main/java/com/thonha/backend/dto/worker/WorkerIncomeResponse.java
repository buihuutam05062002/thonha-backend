package com.thonha.backend.dto.worker;

import com.thonha.backend.dto.response.CommissionSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record WorkerIncomeResponse(
        LocalDate from,
        LocalDate to,
        CommissionSummaryResponse summary,
        List<Daily> daily) {

    public record Daily(
            LocalDate date,
            BigDecimal grossAmount,
            BigDecimal commissionAmount,
            BigDecimal workerAmount,
            int orders) {
    }
}
