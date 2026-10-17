package com.thonha.backend.dto.request;

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
public class CreateQuotationRequest {

    @NotNull(message = "Thiếu yêu cầu cần báo giá")
    Long requestId;

    @NotNull(message = "Vui lòng nhập số tiền báo giá")
    @DecimalMin(value = "1000", message = "Số tiền báo giá tối thiểu là 1.000đ")
    @Digits(integer = 10, fraction = 0, message = "Số tiền báo giá không hợp lệ")
    BigDecimal totalAmount;
    
    @Min(value = 0, message = "Số ngày bảo hành không được âm")
    @Max(value = 3650, message = "Số ngày bảo hành tối đa 3650 ngày")
    Integer warrantyDays;
    
    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    String note;
}
