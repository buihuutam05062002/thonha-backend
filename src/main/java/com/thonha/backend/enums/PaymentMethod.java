package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    CASH("Tiền mặt"),
    BANK_TRANSFER("Chuyển khoản");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }
}
