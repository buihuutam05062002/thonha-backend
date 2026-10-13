package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum ApprovalStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Đã từ chối");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }
}