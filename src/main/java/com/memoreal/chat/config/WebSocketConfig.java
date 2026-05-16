package com.memoreal.chat.config;

import com.memoreal.chat.handler.ChatWebSocketHandler;
import com.memoreal.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtUtil jwtUtil;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    /**
     * WebSocket 연결 시 JWT 토큰 검증 인터셉터
     * 연결 URL: ws://localhost:8080/ws/chat?token={JWT}
     */
    private HandshakeInterceptor jwtHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) {
                String query = request.getURI().getQuery();
                if (query == null) {
                    log.warn("WebSocket 연결 거부 - 토큰 없음");
                    return false;
                }

                // URL 파라미터에서 token 추출: ?token=xxx
                String token = null;
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6);
                        break;
                    }
                }

                if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
                    log.warn("WebSocket 연결 거부 - 유효하지 않은 토큰");
                    return false;
                }

                // kakaoId를 WebSocket 세션 속성에 저장
                String kakaoId = jwtUtil.extractKakaoId(token);
                attributes.put("kakaoId", kakaoId);
                log.info("WebSocket 연결 허용 - kakaoId: {}", kakaoId);
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Exception exception) {}
        };
    }
}
