package com.thonha.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectWarrantyRequest {
    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;
}
