package com.thonha.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    String account;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;
}