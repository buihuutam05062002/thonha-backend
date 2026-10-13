package com.thonha.backend.controller.admin;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.UpsertCommissionPolicyRequest;
import com.thonha.backend.dto.response.CommissionPolicyResponse;
import com.thonha.backend.dto.response.CommissionSettlementResponse;
import com.thonha.backend.dto.response.CommissionSummaryResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.CommissionPolicyService;
import com.thonha.backend.service.PaymentTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CommissionAdminController {

    private final CommissionPolicyService commissionPolicyService;
    private final PaymentTransactionService paymentTransactionService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/commission-policies")
    public ResponseEntity<ApiResponse<CommissionPolicyResponse.Overview>> policies() {
        return ResponseEntity.ok(ApiResponse.success(commissionPolicyService.overview()));
    }

    @PutMapping("/commission-policies")
    public ResponseEntity<ApiResponse<CommissionPolicyResponse>> upsertPolicy(
            @Valid @RequestBody UpsertCommissionPolicyRequest body) {
        return ResponseEntity.ok(ApiResponse.success(
                commissionPolicyService.upsert(body, currentUserProvider.requireUserId()),
                "Cập nhật tỷ lệ hoa hồng thành công (áp dụng cho các đơn hoàn thành từ bây giờ)"));
    }

    @GetMapping("/commissions")
    public ResponseEntity<ApiResponse<Page<CommissionSettlementResponse>>> settlements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(paymentTransactionService.search(from, to, pageable)));
    }

    @GetMapping("/commissions/summary")
    public ResponseEntity<ApiResponse<CommissionSummaryResponse>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(paymentTransactionService.summarize(from, to)));
    }
}
