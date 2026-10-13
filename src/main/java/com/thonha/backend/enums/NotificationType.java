package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    NEW_REQUEST("Yêu cầu mới"),
    REQUEST_MATCHED("Đã ghép thợ"),
    NEW_QUOTATION("Báo giá mới"),
    STATUS_UPDATE("Cập nhật trạng thái"),
    PAYMENT("Thanh toán"),
    COMPLAINT("Khiếu nại"),
    SYSTEM("Hệ thống");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }
}
