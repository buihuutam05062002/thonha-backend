package com.thonha.backend.dto.request;

import jakarta.validation.constraints.*;
import com.thonha.backend.enums.CategoryStatus;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class CategoryUpdateRequest {
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    String description;

    @Size(max = 255, message = "Icon tối đa 255 ký tự")
    String icon;

    @Min(value = 0, message = "Số ngày bảo hành không được âm")
    @Max(value = 3650, message = "Số ngày bảo hành tối đa 3650 ngày")
    private Integer defaultWarrantyDays;
    
    CategoryStatus status;
}