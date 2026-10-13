package com.thonha.backend.dto.request;

import com.thonha.backend.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class UserSearchRequest {
    String keyword;
    UserStatus userStatus;
    String role;
    LocalDateTime createdFrom;
    LocalDateTime createdTo;
}