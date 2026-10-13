package com.thonha.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.Base64;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessMs;
    private final long refreshMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-minutes:30}") long accessMinutes,
                      @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required");
        }
        byte[] raw = Base64.getDecoder().decode(secret);
        if (raw.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.accessMs = accessMinutes * 60_000L;
        this.refreshMs = refreshDays * 86_400_000L;
    }

    public String accessToken(Long id, Collection<String> roles) {
        return create(id, roles, "access", accessMs);
    }

    public String refreshToken(Long id) {
        return create(id, List.of(), "refresh", refreshMs);
    }

    private String create(Long id, Collection<String> roles, String type, long ttl) {
        var builder = Jwts.builder()
                .subject(id.toString())
                .claim("type", type)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl));
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long accessExpiresIn() {
        return accessMs / 1000;
    }

    public boolean isAccess(String token) {
        return "access".equals(parse(token).get("type", String.class));
    }
}