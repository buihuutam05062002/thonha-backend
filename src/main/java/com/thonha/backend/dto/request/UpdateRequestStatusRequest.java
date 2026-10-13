package com.thonha.backend.dto.request;

import com.thonha.backend.enums.RepairStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class UpdateRequestStatusRequest {

    @NotNull(message = "Thiếu trạng thái mới")
     RepairStatus status;

    @DecimalMin(value = "0", message = "Giá không được âm")
    @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
     BigDecimal finalPrice;
}
