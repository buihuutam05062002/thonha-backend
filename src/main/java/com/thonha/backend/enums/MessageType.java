package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    TEXT("Văn bản"),
    IMAGE("Hình ảnh");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }
}
