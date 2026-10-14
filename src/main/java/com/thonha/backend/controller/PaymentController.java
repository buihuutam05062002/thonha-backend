package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.response.PaymentResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.OnlinePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Thanh toán online phía khách hàng (chỉ khách chủ yêu cầu mới thao tác được). */
@RestController
@RequestMapping("/api/v1/repair-requests/{requestId}/payment")
public class PaymentController {

    private final OnlinePaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public PaymentController(OnlinePaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Thanh toán đang chờ / đã thành công của yêu cầu; data = null nếu chưa có. */
    @GetMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> get(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getForRequest(currentUserProvider.requireUserId(), requestId)));
    }

    /** Tạo (hoặc lấy lại) mã QR chuyển khoản cho báo giá đã chấp nhận. */
    @PostMapping("/bank-transfer")
    public ResponseEntity<ApiResponse<PaymentResponse>> createBankTransfer(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.createBankTransfer(currentUserProvider.requireUserId(), requestId)));
    }
}
