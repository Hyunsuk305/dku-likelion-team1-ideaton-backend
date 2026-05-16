package com.memoreal.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.memoreal.chat.domain.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ChatDto {

    // ── WebSocket으로 주고받는 메시지 ─────────────────────────────────────

    @Getter
    @Builder
    public static class ChatRequest {
        private String sessionId;
        private String message; // 텍스트로 직접 입력 시
    }

    @Getter
    @Builder
    public static class ChatResponse {
        private String sessionId;
        private String role;        // "assistant"
        private String content;     // AI 응답 텍스트
        private boolean isEnd;      // 스트리밍 종료 여부

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static ChatResponse from(ChatMessage message) {
            return ChatResponse.builder()
                    .sessionId(message.getSessionId())
                    .role(message.getRole().name().toLowerCase())
                    .content(message.getContent())
                    .isEnd(true)
                    .createdAt(message.getCreatedAt())
                    .build();
        }
    }

    // ── STT REST API 응답 ─────────────────────────────────────────────────

    @Getter
    @Builder
    public static class SttResponse {
        private String sessionId;
        private String sttText;     // 음성 → 텍스트 변환 결과
        private String aiResponse;  // GPT 응답
    }

    // ── 대화 이력 조회 ────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class MessageHistory {
        private Long id;
        private String role;
        private String content;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static MessageHistory from(ChatMessage msg) {
            return MessageHistory.builder()
                    .id(msg.getId())
                    .role(msg.getRole().name().toLowerCase())
                    .content(msg.getContent())
                    .createdAt(msg.getCreatedAt())
                    .build();
        }
    }
}
