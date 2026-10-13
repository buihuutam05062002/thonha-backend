package com.thonha.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class CommissionCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private CommissionCalculator() {
    }

    public record Result(BigDecimal commissionAmount, BigDecimal workerAmount) {
    }


    public static Result calculate(BigDecimal price, BigDecimal ratePercent) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Giá phải lớn hơn 0");
        }
        if (ratePercent == null || ratePercent.signum() < 0 || ratePercent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Tỷ lệ hoa hồng phải trong khoảng 0 đến 100");
        }
        BigDecimal gross = price.setScale(2, RoundingMode.HALF_UP);
        BigDecimal commission = price.multiply(ratePercent)
                .divide(HUNDRED, 0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);
        if (commission.compareTo(gross) > 0) {
            commission = gross;
        }
        return new Result(commission, gross.subtract(commission));
    }
}
