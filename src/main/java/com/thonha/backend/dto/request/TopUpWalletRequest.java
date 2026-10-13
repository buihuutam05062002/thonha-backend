package com.thonha.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class TopUpWalletRequest {
    @NotNull(message = "Thiếu số tiền")
    @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0")
    @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên (VND)")
    BigDecimal amount;

    @Size(max = 60, message = "Mã tham chiếu tối đa 60 ký tự")
    String referenceCode;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    String note;
}
