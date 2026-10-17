package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.response.PaymentResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.PaymentTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trạng thái thanh toán của đơn phía khách hàng (chỉ khách chủ yêu cầu xem được).
 * Khách thanh toán trực tiếp cho thợ (tiền mặt hoặc chuyển khoản riêng) nên không còn API tạo mã QR cho khách;
 * QR SePay nay dùng để thợ nạp tiền vào ví (xem WorkerWalletController).
 */
@RestController
@RequestMapping("/api/v1/repair-requests/{requestId}/payment")
public class PaymentController {

    private final PaymentTransactionService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public PaymentController(PaymentTransactionService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Thanh toán đã ghi nhận của yêu cầu; data = null nếu thợ chưa chốt giá và hoàn thành đơn. */
    @GetMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> get(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getForRequest(currentUserProvider.requireUserId(), requestId)));
    }
}
