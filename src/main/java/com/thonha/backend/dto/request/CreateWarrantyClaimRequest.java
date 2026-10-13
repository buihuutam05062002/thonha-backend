package com.thonha.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWarrantyClaimRequest {
    @NotNull(message = "Thiếu mã yêu cầu")
    private Long requestId;
    
    @NotBlank(message = "Vui lòng mô tả sự cố")
    @Size(min = 10,max = 1000,message = "Mô tả cần 10 đến 1000 ký tự")
    private String description;
}
