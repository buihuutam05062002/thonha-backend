package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateQuotationRequest;
import com.thonha.backend.dto.response.QuotationResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final CurrentUserProvider currentUserProvider;

    public QuotationController(QuotationService quotationService, CurrentUserProvider currentUserProvider) {
        this.quotationService = quotationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<QuotationResponse>> create(@Valid @RequestBody CreateQuotationRequest body) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                quotationService.create(currentUserProvider.requireUserId(), body)));
    }

    @GetMapping("/by-request/{requestId}")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> byRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.listByRequest(currentUserProvider.requireUserId(), requestId)));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<QuotationResponse>> accept(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.respond(currentUserProvider.requireUserId(), id, true), "Đã chấp nhận báo giá"));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<QuotationResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.respond(currentUserProvider.requireUserId(), id, false), "Đã từ chối báo giá"));
    }
}
