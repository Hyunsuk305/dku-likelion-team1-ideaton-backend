package com.memoreal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoreal.domain.User;
import com.memoreal.dto.AuthResponse;
import com.memoreal.repository.UserRepository;
import com.memoreal.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

   @Override
public void onAuthenticationSuccess(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) throws IOException {
    DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
    String kakaoId = String.valueOf(oAuth2User.getAttributes().get("id"));

    User user = userRepository.findByKakaoId(kakaoId).orElseThrow();
    String accessToken = jwtUtil.generateAccessToken(kakaoId, user.getRole());
    boolean isNewUser = user.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1));

    // JSON 응답 대신 프론트로 리다이렉트 (토큰을 쿼리스트링으로 전달)
    String redirectUrl = "http://localhost:5173/auth/callback"
            + "?token=" + accessToken
            + "&isNewUser=" + isNewUser;

    response.sendRedirect(redirectUrl);
        }
}
