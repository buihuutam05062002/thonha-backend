package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum ComplaintStatus {
    PENDING("Chờ xử lý"),
    PROCESSING("Đang xử lý"),
    RESOLVED("Đã giải quyết"),
    REJECTED("Từ chối");

    private final String description;

    ComplaintStatus(String description) {
        this.description = description;
    }
}
