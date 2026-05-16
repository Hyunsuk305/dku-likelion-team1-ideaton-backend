package com.memoreal.controller;

import com.memoreal.domain.User;
import com.memoreal.dto.ApiResponse;
import com.memoreal.dto.AuthResponse;
import com.memoreal.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    /**
     * 카카오 로그인 URL 반환
     * 앱에서 이 URL을 WebView/CustomTab으로 열면 카카오 로그인 페이지로 이동
     */
    @GetMapping("/kakao/url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getKakaoLoginUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();

        String baseUrl = (port == 80 || port == 443)
                ? scheme + "://" + serverName
                : scheme + "://" + serverName + ":" + port;

        String loginUrl = baseUrl + "/oauth2/authorization/kakao";
        log.info("카카오 로그인 URL 요청: {}", loginUrl);

        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", loginUrl)));
    }

    /**
     * 현재 로그인된 유저 정보 조회
     * Header: Authorization: Bearer {JWT}
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfoResponse>> getMyInfo(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.UserInfoResponse.from(user)));
    }

    /**
     * Access Token 재발급
     * Header: Authorization: Bearer {JWT}
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @AuthenticationPrincipal User user) {
        String newAccessToken = jwtUtil.generateAccessToken(user.getKakaoId(), user.getRole());

        Map<String, Object> tokenData = Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer",
                "expiresIn", jwtUtil.getExpiration()
        );

        return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", tokenData));
    }

    /**
     * 로그아웃
     * 실제 JWT 무효화는 Redis 블랙리스트 또는 클라이언트에서 토큰 삭제로 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User user) {
        log.info("로그아웃 - userId: {}", user.getId());
        // TODO: Redis 블랙리스트에 토큰 추가 (선택적)
        return ResponseEntity.ok(ApiResponse.ok("로그아웃 되었습니다.", null));
    }
}
