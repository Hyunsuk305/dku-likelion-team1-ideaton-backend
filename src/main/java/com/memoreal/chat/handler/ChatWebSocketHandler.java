package com.memoreal.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoreal.chat.dto.ChatDto;
import com.memoreal.chat.service.ChatService;
import com.memoreal.domain.User;
import com.memoreal.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    // 연결된 세션 관리
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket 연결 - sessionId: {}", session.getId());

        // 연결되자마자 AI 첫 인사 전송
        try {
            String kakaoId = (String) session.getAttributes().get("kakaoId");
            User user = (User) userDetailsService.loadUserByUsername(kakaoId);
            String chatSessionId = chatService.createSession();

            // 세션에 chatSessionId 저장
            session.getAttributes().put("chatSessionId", chatSessionId);
            session.getAttributes().put("user", user);

            // 첫 인사 메시지
            String greeting = chatService.processTextMessage(
                    user, chatSessionId,
                    "안녕하세요, 처음 뵙겠습니다. 인터뷰를 시작해주세요."
            );

            sendMessage(session, ChatDto.ChatResponse.builder()
                    .sessionId(chatSessionId)
                    .role("assistant")
                    .content(greeting)
                    .isEnd(true)
                    .build());

        } catch (Exception e) {
            log.error("WebSocket 초기화 실패: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String kakaoId = (String) session.getAttributes().get("kakaoId");
            User user = (User) userDetailsService.loadUserByUsername(kakaoId);
            String chatSessionId = (String) session.getAttributes().get("chatSessionId");

            ChatDto.ChatRequest request = objectMapper.readValue(
                    message.getPayload(), ChatDto.ChatRequest.class
            );

            log.info("WebSocket 메시지 수신 - kakaoId: {}, message: {}", kakaoId, request.getMessage());

            // GPT 응답 생성
            String aiResponse = chatService.processTextMessage(
                    user, chatSessionId, request.getMessage()
            );

            sendMessage(session, ChatDto.ChatResponse.builder()
                    .sessionId(chatSessionId)
                    .role("assistant")
                    .content(aiResponse)
                    .isEnd(true)
                    .build());

        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 실패: {}", e.getMessage(), e);
            sendError(session, "메시지 처리 중 오류가 발생했습니다.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket 연결 종료 - sessionId: {}, status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 전송 오류 - sessionId: {}, error: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    // ── Private 헬퍼 ────────────────────────────────────────────────────

    private void sendMessage(WebSocketSession session, ChatDto.ChatResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            String json = objectMapper.writeValueAsString(
                    ChatDto.ChatResponse.builder()
                            .role("error")
                            .content(errorMessage)
                            .isEnd(true)
                            .build()
            );
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("WebSocket 에러 전송 실패: {}", e.getMessage());
        }
    }
}
