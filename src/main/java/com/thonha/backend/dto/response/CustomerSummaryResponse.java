package com.thonha.backend.dto.response;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerSummaryResponse(
        List<PendingQuotation> pendingQuotations,
        List<AwaitingReview> awaitingReview
) {
    public record PendingQuotation(Long quotationId, Long requestId, String requestCode,
                                   String categoryName, String workerName,
                                   BigDecimal totalAmount, LocalDateTime createdAt) {}

    public record AwaitingReview(Long requestId, String requestCode, String categoryName,
                                 String workerName, LocalDateTime completedAt) {}
}