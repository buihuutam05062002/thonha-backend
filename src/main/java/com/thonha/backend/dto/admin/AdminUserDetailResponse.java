package com.thonha.backend.dto.admin;

import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.User;
import com.thonha.backend.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record AdminUserDetailResponse(
        Long id,
        String fullName,
        String username,
        String email,
        String phoneNumber,
        String avatarUrl,
        UserStatus status,
        Set<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminUserDetailResponse from(User u) {
        return new AdminUserDetailResponse(
                u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getPhoneNumber(),
                u.getAvatarUrl(), u.getStatus(),
                u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                u.getCreatedAt(), u.getUpdatedAt());
    }
}
