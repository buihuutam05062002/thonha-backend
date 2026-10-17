package com.thonha.backend.dto.request;

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
public class CreateTopUpRequest {
    @NotNull(message = "Vui lòng nhập số tiền cần nạp")
    @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0")
    @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên (VND)")
    BigDecimal amount;
}
