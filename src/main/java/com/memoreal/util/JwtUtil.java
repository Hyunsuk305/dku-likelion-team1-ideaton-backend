package com.memoreal.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    // ── Access Token 생성 ────────────────────────────────────────────────
    public String generateAccessToken(String kakaoId, String role) {
        return buildToken(kakaoId, role, expiration);
    }

    // ── Refresh Token 생성 ───────────────────────────────────────────────
    public String generateRefreshToken(String kakaoId) {
        return buildToken(kakaoId, null, refreshExpiration);
    }

    // ── 토큰에서 kakaoId 추출 ────────────────────────────────────────────
    public String extractKakaoId(String token) {
        return getClaims(token).getSubject();
    }

    // ── 토큰에서 role 추출 ───────────────────────────────────────────────
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    // ── 토큰 만료 시간 (ms) ──────────────────────────────────────────────
    public long getExpiration() {
        return expiration;
    }

    // ── 토큰 유효성 검사 ─────────────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }

    // ── Private 헬퍼 ────────────────────────────────────────────────────
    private String buildToken(String subject, String role, Long validity) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getKey(), SignatureAlgorithm.HS256);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
