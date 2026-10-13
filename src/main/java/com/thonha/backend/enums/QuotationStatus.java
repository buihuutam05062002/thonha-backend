package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum QuotationStatus {
    PENDING("Chờ khách phản hồi"),
    ACCEPTED("Đã chấp nhận"),
    REJECTED("Đã từ chối"),
    EXPIRED("Hết hạn");

    private final String description;

    QuotationStatus(String description) {
        this.description = description;
    }
}
