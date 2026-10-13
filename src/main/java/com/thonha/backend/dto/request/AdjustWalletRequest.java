package com.thonha.backend.dto.request;

import com.thonha.backend.enums.WalletTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
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
public class AdjustWalletRequest {
    @NotNull(message = "Thiếu loại điều chỉnh")
    WalletTransactionType type;

    @NotNull(message = "Thiếu số tiền")
    @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0")
    @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên (VND)")
    BigDecimal amount;

    @NotBlank(message = "Vui lòng nhập lý do điều chỉnh")
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    String reason;
}
