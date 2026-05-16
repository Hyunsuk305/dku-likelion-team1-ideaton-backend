package com.memoreal.controller;

import com.memoreal.domain.LoginType;
import com.memoreal.domain.User;
import com.memoreal.repository.UserRepository;
import com.memoreal.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .kakaoId("test-kakao-123")
                .email("test@kakao.com")
                .nickname("테스트유저")
                .loginType(LoginType.KAKAO)
                .build();
        userRepository.save(testUser);

        accessToken = jwtUtil.generateAccessToken(testUser.getKakaoId(), testUser.getRole());
    }

    @Test
    @DisplayName("카카오 로그인 URL 반환 성공")
    void getKakaoLoginUrl() throws Exception {
        mockMvc.perform(get("/api/auth/kakao/url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    @DisplayName("내 정보 조회 성공 - 유효한 JWT")
    void getMyInfo_success() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("테스트유저"));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 토큰 없음")
    void getMyInfo_unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
