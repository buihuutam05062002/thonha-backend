package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum ComplaintReason {
    POOR_QUALITY("Chất lượng kém"),
    OVERCHARGED("Tính phí sai"),
    LATE_ARRIVAL("Đến trễ"),
    NO_SHOW("Thợ không đến"),
    RUDE_BEHAVIOR("Thái độ không tốt"),
    OTHER("Lý do khác");

    private final String description;

    ComplaintReason(String description) {
        this.description = description;
    }
}
