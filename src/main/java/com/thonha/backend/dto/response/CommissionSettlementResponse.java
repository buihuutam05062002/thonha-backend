package com.thonha.backend.dto.response;

import com.thonha.backend.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommissionSettlementResponse(
        Long id,
        Long requestId,
        String requestCode,
        Long workerId,
        String workerName,
        BigDecimal amount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal workerAmount,
        String paymentMethod,
        LocalDateTime createdAt) {

    public static CommissionSettlementResponse from(PaymentTransaction t) {
        var request = t.getRequest();
        var worker = request.getWorker();
        return new CommissionSettlementResponse(
                t.getId(), request.getId(), request.getRequestCode(),
                worker == null ? null : worker.getId(),
                worker == null ? null : worker.getUser().getFullName(),
                t.getAmount(), t.getCommissionRate(), t.getCommissionAmount(), t.getWorkerAmount(),
                t.getPaymentMethod().name(), t.getCreatedAt());
    }
}
