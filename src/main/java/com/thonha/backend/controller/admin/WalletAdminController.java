package com.thonha.backend.controller.admin;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.AdjustWalletRequest;
import com.thonha.backend.dto.request.TopUpWalletRequest;
import com.thonha.backend.dto.response.WalletResponse;
import com.thonha.backend.dto.response.WalletTransactionResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/wallets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class WalletAdminController {

    private final WalletService walletService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WalletResponse>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean negativeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(walletService.searchWallets(
                keyword, negativeOnly, PageRequest.of(Math.max(page, 0), clamp(size)))));
    }

    @GetMapping("/{workerId}")
    public ResponseEntity<ApiResponse<WalletResponse>> detail(@PathVariable Long workerId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(workerId)));
    }

    @GetMapping("/{workerId}/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> transactions(
            @PathVariable Long workerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.getTransactions(workerId, PageRequest.of(Math.max(page, 0), clamp(size)))));
    }

    @PostMapping("/{workerId}/top-up")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> topUp(
            @PathVariable Long workerId, @Valid @RequestBody TopUpWalletRequest body) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.topUp(workerId, body.getAmount(), body.getReferenceCode(), body.getNote(),
                        currentUserProvider.requireUserId()),
                "Nạp tiền vào ví thành công"));
    }

    @PostMapping("/{workerId}/adjustments")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> adjust(
            @PathVariable Long workerId, @Valid @RequestBody AdjustWalletRequest body) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.adjust(workerId, body.getType(), body.getAmount(), body.getReason(),
                        currentUserProvider.requireUserId()),
                "Điều chỉnh số dư thành công"));
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
