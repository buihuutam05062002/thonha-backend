package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum WithdrawalStatus {
    PENDING("Chờ xử lý"),
    SUCCESS("Thành công"),
    FAILED("Thất bại");

    private final String description;

    WithdrawalStatus(String description) {
        this.description = description;
    }
}
