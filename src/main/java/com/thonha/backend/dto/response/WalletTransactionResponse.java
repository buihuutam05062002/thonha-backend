package com.thonha.backend.dto.response;

import com.thonha.backend.entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long id,
        String type,
        String typeLabel,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long requestId,
        String note,
        Long createdBy,
        LocalDateTime createdAt) {

    public static WalletTransactionResponse from(WalletTransaction t) {
        return new WalletTransactionResponse(t.getId(), t.getType().name(), t.getType().getDescription(),
                t.getAmount(), t.getBalanceAfter(), t.getRequestId(), t.getNote(), t.getCreatedBy(), t.getCreatedAt());
    }
}
