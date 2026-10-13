package com.thonha.backend.dto.worker;

import java.math.BigDecimal;
import java.util.Map;


public record WorkerReviewSummaryResponse(
        BigDecimal averageRating,
        long totalReviews,
        Map<Integer, Long> distribution) {
}
