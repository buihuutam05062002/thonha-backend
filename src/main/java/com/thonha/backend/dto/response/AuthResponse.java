package com.thonha.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class AuthResponse {
    String accessToken;
    String refreshToken;
    long accessExpiresIn;
    UserResponse user;
}