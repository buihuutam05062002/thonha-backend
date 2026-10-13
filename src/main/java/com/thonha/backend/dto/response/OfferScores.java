package com.thonha.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferScores {
    private BigDecimal totalScore;
    private BigDecimal distanceScore;
    private BigDecimal ratingScore;
    private BigDecimal acceptanceRateScore;
    private BigDecimal workloadScore;
}
