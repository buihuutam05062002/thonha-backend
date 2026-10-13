package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum DiscountType {
    PERCENTAGE("Giảm theo phần trăm"),
    FIXED_AMOUNT("Giảm theo số tiền");

    private final String description;

    DiscountType(String description) {
        this.description = description;
    }
}
