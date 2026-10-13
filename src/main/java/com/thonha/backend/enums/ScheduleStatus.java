package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum ScheduleStatus {
    AVAILABLE("Sẵn sàng"),
    BOOKED("Đã đặt"),
    DAY_OFF("Tạm nghỉ");

    private final String description;

    ScheduleStatus(String description) {
        this.description = description;
    }
}
