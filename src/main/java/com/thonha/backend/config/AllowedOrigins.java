package com.thonha.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách origin của trình duyệt được phép gọi backend. Dùng chung cho CORS (REST) và WebSocket để hai bên
 * không lệch nhau (trước đây WebSocket thiếu origin mà CORS có, nên bắt tay WebSocket bị 403).
 * <p>
 * Thêm origin khác qua biến môi trường {@code CORS_ALLOWED_ORIGINS}, nhiều giá trị cách nhau bằng dấu phẩy,
 * hỗ trợ ký tự đại diện, ví dụ: {@code https://*.ngrok-free.dev,http://192.168.1.20:5173}.
 */
@Component
public class AllowedOrigins {

    private final List<String> patterns;

    public AllowedOrigins(@Value("${app.cors.allowed-origins:}") String[] extra) {
        List<String> all = new ArrayList<>(List.of(
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*",
                "https://127.0.0.1:*",
                "http://192.168.1.2:*",
                "https://192.168.1.2:*",
                "http://192.168.1.5:*",
                "https://192.168.1.5:*",
                "https://patio-yelp-retiree.ngrok-free.dev"
        )


        );
        for (String origin : extra) {
            if (origin != null && !origin.isBlank()) {
                all.add(origin.trim());
            }
        }
        this.patterns = List.copyOf(all);
    }

    public List<String> patterns() {
        return patterns;
    }

    public String[] asArray() {
        return patterns.toArray(new String[0]);
    }
}
