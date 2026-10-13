package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum MatchingResult {
    ACCEPTED("Chấp nhận"),
    REJECTED("Từ chối"),
    PENDING("Chờ phản hồi"),
    EXPIRED("Hết hạn"),
    CANCELLED_MIDWAY("Hủy giữa chừng");

    private final String description;

    MatchingResult(String description) {
        this.description = description;
    }
}
