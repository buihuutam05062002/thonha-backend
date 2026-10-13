package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum PriorityLevel {
    LOW("Thấp"),
    MEDIUM("Trung bình"),
    HIGH("Cao"),
    URGENT("Khẩn cấp");

    private final String description;

    PriorityLevel(String description) {
        this.description = description;
    }
}
