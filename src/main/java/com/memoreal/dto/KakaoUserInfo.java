package com.memoreal.dto;

import lombok.Getter;

import java.util.Map;

@Getter
public class KakaoUserInfo {

    private String id;
    private String email;
    private String nickname;
    private String profileImage;

    @SuppressWarnings("unchecked")
    public static KakaoUserInfo from(Map<String, Object> attributes) {
        KakaoUserInfo info = new KakaoUserInfo();
        info.id = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount != null) {
            info.email = (String) kakaoAccount.getOrDefault("email", "");

            Map<String, Object> profile =
                    (Map<String, Object>) kakaoAccount.get("profile");

            if (profile != null) {
                info.nickname = (String) profile.get("nickname");
                info.profileImage = (String) profile.get("profile_image_url");
            }
        }

        return info;
    }
}
