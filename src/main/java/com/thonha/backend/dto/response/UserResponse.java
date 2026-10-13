package com.thonha.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class UserResponse {
    Long id;
    String fullName;
    String email;
    String phoneNumber;
    String avatarUrl;
    String status;
    Set<String> roles;

    public static UserResponse from(com.thonha.backend.entity.User user) {
        if (user == null) return null;
        Set<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet())
                : Set.of();
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(roles)
                .build();
    }
}