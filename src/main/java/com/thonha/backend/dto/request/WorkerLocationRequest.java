package com.thonha.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record WorkerLocationRequest(
        @NotNull(message = "Vĩ độ là bắt buộc")
        @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
        @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
        Double lat,

        @NotNull(message = "Kinh độ là bắt buộc")
        @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
        @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
        Double lng) {
}
