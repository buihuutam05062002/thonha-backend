package com.thonha.backend.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(@NotBlank @Size(max = 100) String fullName, @Email @Size(max = 150) String email,
                              @Pattern(regexp = "^$|^0[0-9]{9,10}$") String phoneNumber,
                              @NotBlank @Size(min = 6, max = 72) String password) {
}
