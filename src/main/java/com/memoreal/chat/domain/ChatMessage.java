package com.memoreal.chat.domain;

import com.memoreal.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 대화 세션 구분 (한 번의 자서전 인터뷰 세션)
    @Column(nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role; // USER / ASSISTANT

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // STT로 변환된 원본 음성 텍스트 (사용자 메시지일 때만)
    @Column(columnDefinition = "TEXT")
    private String originalSttText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum MessageRole {
        USER, ASSISTANT
    }
}
