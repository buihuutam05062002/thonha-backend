package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum CategoryStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động");

    private final String description;

    CategoryStatus(String description) {
        this.description = description;
    }
}