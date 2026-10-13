package com.thonha.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {
    private Long offerId;
    private Long requestId;
    private String requestCode;
    private String categoryName;
    private String description;
    private String addressText;
    private BigDecimal lat;
    private BigDecimal lng;
    private String priorityLevel;
    private String customerName;
    private String customerPhone;
    private String requestStatus;
    private List<String> attachments;
    private Double distance;
    private String result;
    private int remainingSeconds;
}
