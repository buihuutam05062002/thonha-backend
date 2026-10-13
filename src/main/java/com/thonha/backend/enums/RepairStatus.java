package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum RepairStatus {
    PENDING_MATCH("Chờ ghép thợ"),
    MATCHING("Đang ghép thợ"),
    MATCHED("Đã ghép thợ"),
    ON_THE_WAY("Đang di chuyển"),
    IN_PROGRESS("Đang sửa chữa"),
    COMPLETED("Hoàn thành"),
    NOT_FOUND("Không tìm thấy thợ"),
    CANCELLED("Đã hủy");

    private final String description;

    RepairStatus(String description) {
        this.description = description;
    }
}