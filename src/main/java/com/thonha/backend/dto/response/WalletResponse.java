package com.thonha.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long walletId,
        Long workerId,
        String workerName,
        String workerPhone,
        BigDecimal balance,
        BigDecimal minBalanceToReceiveJobs,
        boolean canReceiveJobs,
        LocalDateTime updatedAt) {
}
