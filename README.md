# Memoreal Backend - 인증 모듈

## 🗂️ 패키지 구조

```
src/main/java/com/memoreal/
├── MemorealApplication.java
├── config/
│   ├── SecurityConfig.java          # Spring Security + OAuth2 설정
│   ├── OAuth2SuccessHandler.java    # 카카오 로그인 성공 후 JWT 발급
│   └── GlobalExceptionHandler.java  # 전역 예외 처리
├── controller/
│   └── AuthController.java          # 인증 API
├── domain/
│   ├── User.java                    # 유저 엔티티
│   └── LoginType.java               # KAKAO / PHONE
├── dto/
│   ├── AuthResponse.java            # 로그인 응답 DTO
│   ├── KakaoUserInfo.java           # 카카오 유저 정보 파싱
│   └── ApiResponse.java             # 공통 응답 래퍼
├── filter/
│   └── JwtAuthFilter.java           # JWT 인증 필터
├── repository/
│   └── UserRepository.java
├── service/
│   └── CustomOAuth2UserService.java # 카카오 유저 저장/조회
└── util/
    └── JwtUtil.java                 # JWT 생성/검증
```

## 🚀 시작하기

### 1. 카카오 개발자 센터 설정
1. https://developers.kakao.com 접속
2. 애플리케이션 생성
3. **카카오 로그인** 활성화
4. **Redirect URI** 등록:
   - 개발: `http://localhost:8080/login/oauth2/code/kakao`
   - 운영: `https://your-domain.com/login/oauth2/code/kakao`
5. **동의항목** 설정: 닉네임, 프로필 사진, 카카오계정(이메일)


## 📡 API 명세

### 카카오 로그인 URL 조회
```
GET /api/auth/kakao/url

Response:
{
  "success": true,
  "message": "success",
  "data": {
    "url": "http://localhost:8080/oauth2/authorization/kakao"
  }
}
```

### 카카오 OAuth2 콜백 (자동 처리)
```
GET /login/oauth2/code/kakao?code={인가코드}

Response (로그인 성공):
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "isNewUser": true,
  "user": {
    "id": 1,
    "email": "user@kakao.com",
    "nickname": "홍길동",
    "profileImage": "https://...",
    "loginType": "KAKAO",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

### 내 정보 조회
```
GET /api/auth/me
Authorization: Bearer {accessToken}

Response:
{
  "success": true,
  "data": { ...유저 정보 }
}
```

### 토큰 재발급
```
POST /api/auth/refresh
Authorization: Bearer {accessToken}

Response:
{
  "success": true,
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  }
}
```

### 로그아웃
```
POST /api/auth/logout
Authorization: Bearer {accessToken}

Response:
{
  "success": true,
  "message": "로그아웃 되었습니다.",
  "data": null
}
``
