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

### 2. 환경변수 설정

```bash
export KAKAO_CLIENT_ID=your_rest_api_key
export KAKAO_CLIENT_SECRET=your_client_secret
export JWT_SECRET=your-256bit-secret-key-here-must-be-long-enough
export DB_USERNAME=root
export DB_PASSWORD=password
```

또는 `.env` 파일 생성 (IntelliJ EnvFile 플러그인 사용):
```
KAKAO_CLIENT_ID=your_rest_api_key
KAKAO_CLIENT_SECRET=your_client_secret
JWT_SECRET=memoreal-secret-key-must-be-at-least-256-bits-long
```

### 3. 빌드 & 실행

```bash
# 로컬 (H2 인메모리 DB)
./gradlew bootRun --args='--spring.profiles.active=local'

# 운영 (MySQL)
./gradlew bootRun
```

---

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
```

---

## 📱 앱(클라이언트) 연동 방법

```
1. GET /api/auth/kakao/url 로 로그인 URL 받기
2. 해당 URL을 CustomTab 또는 WebView로 열기
3. 카카오 로그인 완료 시 콜백 URL로 리다이렉트
4. 서버가 JWT를 JSON으로 응답
5. 앱에서 accessToken 저장 (Secure Storage)
6. 이후 모든 API 요청 헤더에 추가:
   Authorization: Bearer {accessToken}
```

---

## 🔜 다음 단계
- [ ] STT + AI 채팅 API (WebSocket)
- [ ] 전화번호 로그인 (SMS 인증)
- [ ] Redis 토큰 블랙리스트
- [ ] Refresh Token 구현
