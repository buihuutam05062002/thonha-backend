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
}
