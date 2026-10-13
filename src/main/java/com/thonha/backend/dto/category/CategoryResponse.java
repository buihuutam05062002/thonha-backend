package com.thonha.backend.dto.category;

import com.thonha.backend.entity.ServiceCategory;

public record CategoryResponse(Long id, String name, String description, String icon) {
    public static CategoryResponse from(ServiceCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon());
    }
}
