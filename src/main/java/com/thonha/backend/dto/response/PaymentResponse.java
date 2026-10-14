package com.thonha.backend.dto.response;

import com.thonha.backend.enums.PaymentMethod;
import com.thonha.backend.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin thanh toán của một yêu cầu. Khi đang chờ thanh toán chuyển khoản, {@code qrUrl} là ảnh QR
 * (đã có sẵn số tiền + nội dung); khi đã thanh toán thì {@code qrUrl} là null.
 */
public record PaymentResponse(
        Long paymentId,
        Long requestId,
        BigDecimal amount,
        TransactionStatus status,
        String statusDescription,
        PaymentMethod method,
        String paymentCode,
        String qrUrl,
        String bankName,
        String bankAccount,
        String accountHolder,
        LocalDateTime createdAt
) {
}
