package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum PromotionStatus {
    ACTIVE("Hoạt động"),
    PAUSED("Tạm hoãn"),
    EXPIRED("Hết hạn");

    private final String description;

    PromotionStatus(String description) {
        this.description = description;
    }
}
