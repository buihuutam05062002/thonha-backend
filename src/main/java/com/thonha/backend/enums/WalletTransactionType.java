package com.thonha.backend.enums;

import lombok.Getter;

/** Loại bút toán trong sổ cái ví. */
@Getter
public enum WalletTransactionType {
    TOPUP("Nạp tiền"),
    COMMISSION("Trừ hoa hồng"),
    ADJUSTMENT_CREDIT("Điều chỉnh tăng"),
    ADJUSTMENT_DEBIT("Điều chỉnh giảm"),
    JOB_EARNING("Thu nhập đơn thanh toán online");

    private final String description;

    WalletTransactionType(String description) {
        this.description = description;
    }
}
