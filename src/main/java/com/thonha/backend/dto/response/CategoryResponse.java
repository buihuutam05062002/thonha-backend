package com.thonha.backend.dto.response;

import com.thonha.backend.enums.CategoryStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class CategoryResponse {
    Long id;
    String name;
    String description;
    String icon;
    int defaultWarrantyDays;
    CategoryStatus status;

    public static CategoryResponse from(com.thonha.backend.entity.ServiceCategory c) {
        if (c == null) return null;
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .icon(c.getIcon())
                .defaultWarrantyDays(c.getDefaultWarrantyDays())
                .status(c.getStatus())
                .build();
    }
}