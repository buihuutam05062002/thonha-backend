package com.thonha.backend.dto.request;

import com.thonha.backend.enums.ComplaintReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateComplaintRequest {
    @NotNull(message = "Vui lòng chọn yêu cầu cần khiếu nại")
    Long requestId;

    @NotNull(message = "Vui lòng chọn lý do khiếu nại")
    ComplaintReason reason;

    @NotBlank(message = "Vui lòng mô tả vấn đề")
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    String description;
}
