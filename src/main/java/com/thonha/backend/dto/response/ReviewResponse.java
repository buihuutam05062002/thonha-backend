package com.thonha.backend.dto.response;

import com.thonha.backend.entity.Review;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class ReviewResponse {
    Long id;
    Long requestId;
    String customerName;
    String workerName;
    Integer rating;
    String comment;
    LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .requestId(r.getRequest().getId())
                .customerName(r.getCustomer().getFullName())
                .workerName(r.getWorker().getUser().getFullName())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
