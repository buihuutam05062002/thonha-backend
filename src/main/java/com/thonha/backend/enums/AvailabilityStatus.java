package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum AvailabilityStatus {
    READY("Sẵn sàng"),
    OFFLINE("Ngoại tuyến"),
    BUSY("Đang bận");

    private final String description;

    AvailabilityStatus(String description) {
        this.description = description;
    }
}