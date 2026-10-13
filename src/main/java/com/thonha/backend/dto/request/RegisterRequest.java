package com.thonha.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    String fullName;

    @Email
    @Size(max = 150)
    String email;

    @Pattern(regexp = "^$|^0[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    String phoneNumber;

    @NotBlank
    @Size(min = 6, max = 100)
    String password;

    @Size(max = 100)
    String username;
}