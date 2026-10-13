package com.thonha.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AdminStatisticsResponse(
        LocalDate from,
        LocalDate to,
        Users users,
        Requests requests,
        CommissionSummaryResponse revenue,
        Map<String, Long> complaints,
        Reviews reviews,
        List<Daily> daily,
        List<CategoryCount> topCategories,
        List<TopWorker> topWorkers) {

    public record Users(
            long totalCustomers,
            long approvedWorkers,
            long pendingWorkerProfiles,
            long newCustomers,
            long newWorkerProfiles) {
    }

    public record Requests(
            long total,
            long completed,
            long cancelled,
            double completionRate,
            Map<String, Long> byStatus) {
    }

    public record Reviews(double averageRating, long count) {
    }

    public record Daily(
            LocalDate date,
            long requests,
            long orders,
            BigDecimal grossAmount,
            BigDecimal commissionAmount) {
    }

    public record CategoryCount(String name, long count) {
    }

    public record TopWorker(
            Long workerId,
            String fullName,
            long orders,
            BigDecimal grossAmount,
            BigDecimal commissionAmount) {
    }
}
