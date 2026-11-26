package com.factory.report.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 签发与解析，payload: uid/username/role/dept（API.md §10） */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours}") long expireHours) {
        // fail-fast：密钥未配置（或仍为仓库默认占位值）时拒绝启动，防止用公开密钥伪造 token
        if (secret == null || secret.isBlank() || secret.startsWith("CHANGE_ME")) {
            throw new IllegalStateException("JWT 密钥未配置：请通过环境变量 APP_JWT_SECRET 注入 64 位以上随机字符串");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireHours * 3600_000;
    }

    public String sign(Long uid, String username, String role, String dept) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(uid))
                .claim("username", username)
                .claim("role", role)
                .claim("dept", dept)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key)
                .compact();
    }

    /** 解析并校验；失败返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
