package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.UpdateProfileRequest;
import com.thonha.backend.dto.response.UserResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Long userId = currentUserProvider.requireUserId();
        UserResponse response = authService.getMe(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = currentUserProvider.requireUserId();
        UserResponse response = authService.updateProfile(userId, request.getFullName(), request.getAvatarUrl());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hồ sơ thành công"));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(
                authService.updateAvatar(userId, file), "Cập nhật ảnh đại diện thành công"));
    }
}