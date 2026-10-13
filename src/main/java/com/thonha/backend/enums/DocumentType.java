package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    CCCD_FRONT("CCCD mặt trước"),
    CCCD_BACK("CCCD mặt sau"),
    CERTIFICATE("Chứng chỉ"),
    DEGREE("Bằng cấp");

    private final String description;

    DocumentType(String description) {
        this.description = description;
    }
}