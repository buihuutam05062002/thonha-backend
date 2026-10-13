package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum WarrantyStatus {
    PENDING("Chờ tiếp nhận"),
    PROCESSING("Đang xử lý"),
    COMPLETED("Đã hoàn thành"),
    REJECTED("Đã từ chối");
    
    private final String description;
    
    WarrantyStatus(String description) {
        this.description = description;
    }
}
