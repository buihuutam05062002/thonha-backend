package com.thonha.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptWarrantyRequest {
    @NotNull(message = "Vui lòng chọn ngày hẹn")
    private LocalDate appointmentDate;
    
    @NotBlank(message = "Vui lòng chọn khung giờ")
    private String appointmentTime;
    
    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
