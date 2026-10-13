package com.thonha.backend.dto.request;

import com.thonha.backend.enums.ComplaintStatus;
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
public class ResolveComplaintRequest {
    @NotNull(message = "Vui lòng chọn kết quả xử lý")
    ComplaintStatus status;

    @NotBlank(message = "Vui lòng nhập nội dung kết luận")
    @Size(max = 2000, message = "Nội dung kết luận tối đa 2000 ký tự")
    String resolution;
}
