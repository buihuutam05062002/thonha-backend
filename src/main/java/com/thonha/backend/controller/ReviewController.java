package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateReviewRequest;
import com.thonha.backend.dto.response.ReviewResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    public ReviewController(ReviewService reviewService, CurrentUserProvider currentUserProvider) {
        this.reviewService = reviewService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody CreateReviewRequest body) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                reviewService.create(currentUserProvider.requireUserId(), body), "Cảm ơn bạn đã đánh giá"));
    }

    @GetMapping("/by-request/{requestId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> byRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getByRequest(currentUserProvider.requireUserId(), requestId)));
    }
}
