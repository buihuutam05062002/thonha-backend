package com.thonha.backend.controller.worker;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateTopUpRequest;
import com.thonha.backend.dto.response.TopUpResponse;
import com.thonha.backend.dto.response.WalletResponse;
import com.thonha.backend.dto.response.WalletTransactionResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WalletService;
import com.thonha.backend.service.WalletTopUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker/wallet")
@PreAuthorize("hasRole('WORKER')")
@RequiredArgsConstructor
public class WorkerWalletController {

    private final WalletService walletService;
    private final WalletTopUpService topUpService;
    private final CurrentUserProvider currentUserProvider;
    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getMyWallet(currentUserProvider.requireUserId())));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getMyTransactions(
                currentUserProvider.requireUserId(), PageRequest.of(Math.max(page, 0), clamp(size)))));
    }

    /** Thợ tạo lệnh nạp ví: trả về mã QR (đã có số tiền + nội dung) để chuyển khoản. */
    @PostMapping("/top-ups")
    public ResponseEntity<ApiResponse<TopUpResponse>> createTopUp(@Valid @RequestBody CreateTopUpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                topUpService.create(currentUserProvider.requireUserId(), request.getAmount())));
    }

    /** Trạng thái lệnh nạp (giao diện hỏi lại định kỳ cho tới khi SePay báo tiền đã về). */
    @GetMapping("/top-ups/{id}")
    public ResponseEntity<ApiResponse<TopUpResponse>> getTopUp(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                topUpService.get(currentUserProvider.requireUserId(), id)));
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
