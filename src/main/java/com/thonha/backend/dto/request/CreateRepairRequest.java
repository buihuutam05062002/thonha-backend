package com.thonha.backend.dto.request;

import com.thonha.backend.enums.PriorityLevel;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class CreateRepairRequest {
    @NotNull(message = "Danh mục dịch vụ là bắt buộc")
    Long categoryId;

    Long addressId;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 10, max = 2000, message = "Mô tả cần từ 10 đến 2000 ký tự")
    String description;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    String addressText;

    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    Double lat;

    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    Double lng;

    @NotNull(message = "Mức độ ưu tiên là bắt buộc")
    PriorityLevel priorityLevel;
}
