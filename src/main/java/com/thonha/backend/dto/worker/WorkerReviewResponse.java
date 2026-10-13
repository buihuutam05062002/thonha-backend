package com.thonha.backend.dto.worker;

import com.thonha.backend.entity.Review;

import java.time.LocalDateTime;

public record WorkerReviewResponse(
        Long id,
        Long requestId,
        String requestCode,
        String customerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt) {

    public static WorkerReviewResponse from(Review r) {
        return new WorkerReviewResponse(
                r.getId(), r.getRequest().getId(), r.getRequest().getRequestCode(),
                r.getCustomer().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
