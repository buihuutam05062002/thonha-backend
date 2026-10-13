package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum AttachmentType {
    IMAGE("Hình ảnh"),
    VIDEO("Video"),
    DOCUMENT("Tài liệu");

    private final String description;

    AttachmentType(String description) {
        this.description = description;
    }
}
