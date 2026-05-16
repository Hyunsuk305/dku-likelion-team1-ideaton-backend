package com.memoreal.service;

import com.memoreal.domain.LoginType;
import com.memoreal.domain.User;
import com.memoreal.dto.KakaoUserInfo;
import com.memoreal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 시도 - provider: {}", registrationId);

        KakaoUserInfo kakaoUserInfo = KakaoUserInfo.from(oAuth2User.getAttributes());
        log.info("카카오 유저 정보 - kakaoId: {}, nickname: {}", kakaoUserInfo.getId(), kakaoUserInfo.getNickname());

        User user = saveOrUpdate(kakaoUserInfo);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                oAuth2User.getAttributes(),
                "id"
        );
    }

    private User saveOrUpdate(KakaoUserInfo info) {
        return userRepository.findByKakaoId(info.getId())
                .map(existingUser -> {
                    // 기존 유저 - 프로필 업데이트
                    existingUser.updateProfile(info.getNickname(), info.getProfileImage());
                    log.info("기존 유저 업데이트 - kakaoId: {}", info.getId());
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 신규 유저 등록
                    User newUser = User.builder()
                            .kakaoId(info.getId())
                            .email(info.getEmail())
                            .nickname(info.getNickname())
                            .profileImage(info.getProfileImage())
                            .loginType(LoginType.KAKAO)
                            .build();
                    log.info("신규 유저 등록 - kakaoId: {}", info.getId());
                    return userRepository.save(newUser);
                });
    }
}
