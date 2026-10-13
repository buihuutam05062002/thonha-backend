package com.thonha.backend.security;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Long id) {
            return id;
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục");
    }
}