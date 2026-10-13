package com.thonha.backend.controller.admin;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.UserResponse;
import com.thonha.backend.dto.admin.AdminUserDetailResponse;
import com.thonha.backend.dto.request.UserSearchRequest;
import com.thonha.backend.entity.Role;
import com.thonha.backend.repository.RefreshTokenRepository;
import com.thonha.backend.repository.UserSpecifications;
import com.thonha.backend.security.CurrentUserProvider;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {
    private final UserRepository repo;
    private final RefreshTokenRepository refreshTokens;
    private final CurrentUserProvider currentUser;

    public UserAdminController(UserRepository r, RefreshTokenRepository refreshTokens, CurrentUserProvider currentUser) {
        this.repo = r;
        this.refreshTokens = refreshTokens;
        this.currentUser = currentUser;
    }

    @GetMapping
    Page<UserResponse> list(UserSearchRequest search, Pageable p) {
        return repo.findAll(UserSpecifications.search(search), p).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    AdminUserDetailResponse detail(@PathVariable Long id) {
        return repo.findById(id)
                .map(AdminUserDetailResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    UserResponse status(@PathVariable Long id, @RequestParam UserStatus status) {
        var u = repo.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (u.getId().equals(currentUser.requireUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Không thể thay đổi trạng thái tài khoản của chính mình");
        }
        if (status != UserStatus.ACTIVE && u.hasRole(Role.ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Không thể khóa hoặc vô hiệu hóa tài khoản quản trị viên");
        }

        u.setStatus(status);
        var saved = repo.save(u);

        if (status != UserStatus.ACTIVE) {
            refreshTokens.revokeAllByUserId(saved.getId(), LocalDateTime.now());
        }
        return UserResponse.from(saved);
    }
}
