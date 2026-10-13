package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    LOCKED("Bị khóa"),
    DELETED("Đã xóa");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }
}