package com.thonha.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
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
public class UpsertCommissionPolicyRequest {
    private Long categoryId;

    @NotNull(message = "Thiếu tỷ lệ hoa hồng")
    @DecimalMin(value = "0", message = "Tỷ lệ không được âm")
    @DecimalMax(value = "100", message = "Tỷ lệ tối đa 100%")
    @Digits(integer = 3, fraction = 2, message = "Tỷ lệ tối đa 2 chữ số thập phân")
    BigDecimal ratePercent;

    Boolean active;
}
