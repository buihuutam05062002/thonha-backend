package com.thonha.backend.dto.request;

import com.thonha.backend.enums.WarrantyResolution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteWarrantyRequest {
    @NotNull(message = "Vui lòng chọn nguyên nhân")
    private WarrantyResolution reasonType;
    
    @NotBlank(message = "Vui lòng mô tả công việc đã xử lý")
    @Size(min = 10,max = 1000,message = "Mô tả cần 10 đến 1000 ký tự")
    private String resolutionText;
}
