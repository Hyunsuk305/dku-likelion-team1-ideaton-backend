package com.memoreal.chat.service;

import com.memoreal.chat.domain.ChatMessage;
import com.memoreal.chat.dto.ChatDto;
import com.memoreal.chat.repository.ChatMessageRepository;
import com.memoreal.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ClovaSpeechService clovaSpeechService;
    private final GptService gptService;

    /**
     * 새 세션 ID 발급
     */
    public String createSession() {
        return UUID.randomUUID().toString();
    }

    /**
     * 음성 파일 → STT → GPT → 저장
     * 앱에서 녹음 후 파일 업로드 시 호출
     */
    @Transactional
    public ChatDto.SttResponse processVoice(User user, String sessionId, MultipartFile audioFile) {
        // 1. CLOVA STT: 음성 → 텍스트
        log.info("STT 처리 시작 - userId: {}, sessionId: {}", user.getId(), sessionId);
        String sttText = clovaSpeechService.transcribe(audioFile);

        // 2. STT 결과를 GPT에 전달해서 응답 받기
        String aiResponse = processText(user, sessionId, sttText, sttText);

        return ChatDto.SttResponse.builder()
                .sessionId(sessionId)
                .sttText(sttText)
                .aiResponse(aiResponse)
                .build();
    }

    /**
     * 텍스트 메시지 → GPT → 저장
     * WebSocket 또는 텍스트 직접 입력 시 호출
     */
    @Transactional
    public String processTextMessage(User user, String sessionId, String userMessage) {
        return processText(user, sessionId, userMessage, null);
    }

    /**
     * 세션의 대화 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ChatDto.MessageHistory> getHistory(String sessionId) {
        return chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(ChatDto.MessageHistory::from)
                .collect(Collectors.toList());
    }

    // ── Private ─────────────────────────────────────────────────────────

    private String processText(User user, String sessionId, String userMessage, String sttText) {
        // 1. 유저 메시지 저장
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .sessionId(sessionId)
                .role(ChatMessage.MessageRole.USER)
                .content(userMessage)
                .originalSttText(sttText)
                .build();
        chatMessageRepository.save(userMsg);

        // 2. 세션 이력 조회 (방금 저장한 메시지 제외한 이전 대화)
        List<ChatMessage> history = chatMessageRepository
                .findTop20BySessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .filter(m -> !m.getId().equals(userMsg.getId()))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());

        // 3. GPT 응답 생성
        log.info("GPT 호출 - sessionId: {}, historySize: {}", sessionId, history.size());
        String aiResponse = gptService.chat(history, userMessage);

        // 4. AI 응답 저장
        ChatMessage assistantMsg = ChatMessage.builder()
                .user(user)
                .sessionId(sessionId)
                .role(ChatMessage.MessageRole.ASSISTANT)
                .content(aiResponse)
                .build();
        chatMessageRepository.save(assistantMsg);

        return aiResponse;
    }
}
