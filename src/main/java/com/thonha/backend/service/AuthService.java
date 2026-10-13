package com.thonha.backend.service;


import com.thonha.backend.common.ApiException;
import  com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.LoginRequest;
import com.thonha.backend.dto.request.RefreshRequest;
import com.thonha.backend.dto.request.RegisterRequest;
import com.thonha.backend.dto.response.AuthResponse;
import com.thonha.backend.dto.response.UserResponse;
import com.thonha.backend.entity.RefreshToken;
import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.User;
import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.RefreshTokenRepository;
import com.thonha.backend.repository.RoleRepository;
import com.thonha.backend.repository.UserRepository;
import com.thonha.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuthService {
    private static final long AVATAR_MAX_BYTES = 10L * 1024 * 1024; // khớp giới hạn 10MB ở FE
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshDays;
    private final CloudinaryService cloudinaryService;
    private final AttachmentValidator attachmentValidator;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       CloudinaryService cloudinaryService,
                       AttachmentValidator attachmentValidator,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        this.cloudinaryService = cloudinaryService;
        this.attachmentValidator = attachmentValidator;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = blank(request.getEmail());
        String phone = blank(request.getPhoneNumber());

        if (email == null && phone == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Email hoặc số điện thoại là bắt buộc");
        }
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS, "Email đã được sử dụng");
        }
        if (phone != null && userRepository.existsByPhoneNumber(phone)) {
            throw new ApiException(ErrorCode.PHONE_EXISTS, "Số điện thoại đã được sử dụng");
        }
        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.USERNAME_EXISTS, "Tên đăng nhập đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(getRole(Role.CUSTOMER));
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String account = request.getAccount().trim();
        User user = account.contains("@")
                ? userRepository.findByEmailIgnoreCase(account).orElse(null)
                : userRepository.findByPhoneNumber(account).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS_LOGIN);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
            }
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hash(request.getRefreshToken());
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (refreshTokenEntity.getRevokedAt() != null || refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshTokenEntity.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }

        refreshTokenEntity.setRevokedAt(LocalDateTime.now());
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            String tokenHash = hash(request.getRefreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(t -> t.setRevokedAt(LocalDateTime.now()));
        }
    }

    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException( ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String fullName, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException( ErrorCode.USER_NOT_FOUND));

        user.setFullName(fullName.trim());
        // avatarUrl không gửi (null) -> giữ avatar hiện tại; gửi chuỗi rỗng -> xóa avatar
        if (avatarUrl != null) {
            user.setAvatarUrl(blank(avatarUrl));
        }
        return UserResponse.from(userRepository.save(user));
    }

    private Role getRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role not configured: " + name));
    }

    @Transactional
    public UserResponse updateAvatar(Long userId, MultipartFile file) {
        attachmentValidator.requireImage(file, AVATAR_MAX_BYTES);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String url = cloudinaryService.upload(file, "nhatho/avatars/" + userId);
        user.setAvatarUrl(url);
        return UserResponse.from(user);
    }

    private AuthResponse issueTokens(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtService.accessToken(user.getId(), roles);
        String refreshTokenValue = jwtService.refreshToken(user.getId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(refreshTokenValue))
                .expiresAt(LocalDateTime.now().plusDays(refreshDays))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return com.thonha.backend.dto.response.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .accessExpiresIn(jwtService.accessExpiresIn())
                .user(UserResponse.from(user))
                .build();
    }

    private static String blank(String x) {
        return x == null || x.isBlank() ? null : x.trim();
    }

    private String hash(String x) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}