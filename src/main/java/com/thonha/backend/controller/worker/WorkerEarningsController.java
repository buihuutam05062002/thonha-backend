package com.thonha.backend.controller.worker;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.worker.WorkerIncomeResponse;
import com.thonha.backend.dto.worker.WorkerReviewResponse;
import com.thonha.backend.dto.worker.WorkerReviewSummaryResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WorkerEarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/worker")
@PreAuthorize("hasRole('WORKER')")
@RequiredArgsConstructor
public class WorkerEarningsController {

    private final WorkerEarningsService workerEarningsService;
    private final CurrentUserProvider currentUserProvider;


    @GetMapping("/income")
    public ResponseEntity<ApiResponse<WorkerIncomeResponse>> income(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                workerEarningsService.income(currentUserProvider.requireUserId(), from, to)));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<WorkerReviewResponse>>> reviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                workerEarningsService.reviews(currentUserProvider.requireUserId(), page, size)));
    }

    @GetMapping("/reviews/summary")
    public ResponseEntity<ApiResponse<WorkerReviewSummaryResponse>> reviewSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                workerEarningsService.reviewSummary(currentUserProvider.requireUserId())));
    }
}
