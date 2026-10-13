package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    PENDING_PAYMENT("Chờ thanh toán"),
    SUCCESS("Thành công"),
    FAILED("Thất bại"),
    CANCELLED("Đã hủy");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }
}
