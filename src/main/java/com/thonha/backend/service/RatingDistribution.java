package com.thonha.backend.service;

import com.thonha.backend.dto.worker.WorkerReviewSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

final class RatingDistribution {

    private RatingDistribution() {
    }


    static WorkerReviewSummaryResponse summarize(Map<Integer, Long> countByRating) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        long total = 0;
        long starSum = 0;
        for (int star = 5; star >= 1; star--) {
            long count = Math.max(countByRating.getOrDefault(star, 0L), 0L);
            distribution.put(star, count);
            total += count;
            starSum += count * star;
        }
        BigDecimal average = total == 0 ? null
                : BigDecimal.valueOf(starSum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        return new WorkerReviewSummaryResponse(average, total, distribution);
    }
}
