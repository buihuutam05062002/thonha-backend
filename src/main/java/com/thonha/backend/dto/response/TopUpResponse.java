package com.thonha.backend.dto.response;

import com.thonha.backend.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lệnh nạp ví. Khi {@code status = PENDING_PAYMENT}, {@code qrUrl} là ảnh QR đã có sẵn số tiền + nội dung
 * chuyển khoản ({@code paymentCode}); đã thanh toán thì {@code qrUrl} = null và {@code balance} là số dư mới.
 */
public record TopUpResponse(
        Long topUpId,
        BigDecimal amount,
        BigDecimal paidAmount,
        TransactionStatus status,
        String statusDescription,
        String paymentCode,
        String qrUrl,
        String bankName,
        String bankAccount,
        String accountHolder,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
