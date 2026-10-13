package com.thonha.backend.enums;

import lombok.Getter;

@Getter
public enum WarrantyResolution {
    TECHNICAL_ERROR("Lỗi thao tác kỹ thuật cũ"),
    COMPONENT_FAULT("Lỗi linh kiện từ NSX"),
    CUSTOMER_USAGE("Lỗi phát sinh mới / Do khách dùng sai");
    
    private final String description;
    
    WarrantyResolution(String description) {
        this.description = description;
    }
}
