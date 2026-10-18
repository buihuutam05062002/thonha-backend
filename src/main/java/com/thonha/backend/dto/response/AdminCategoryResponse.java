package com.thonha.backend.dto.response;

import com.thonha.backend.enums.CategoryStatus;

public record AdminCategoryResponse(
        Long id,
        String name,
        String description,
        String icon,
        CategoryStatus status,
        long requestCount) {
}
