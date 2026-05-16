package com.memoreal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.memoreal.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private boolean isNewUser;
    private UserInfoResponse user;

    @Getter
    @Builder
    public static class UserInfoResponse {
        private Long id;
        private String email;
        private String nickname;
        private String profileImage;
        private String loginType;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static UserInfoResponse from(User user) {
            return UserInfoResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .loginType(user.getLoginType().name())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }
}
