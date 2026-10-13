package com.thonha.backend.dto;

import com.thonha.backend.entity.*;
import com.thonha.backend.enums.UserStatus;

import java.util.*;

public record UserResponse(Long id, String fullName, String email, String phoneNumber, String avatarUrl,
                           UserStatus status, Set<String> roles) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhoneNumber(), u.getAvatarUrl(), u.getStatus(), u.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
    }
}
