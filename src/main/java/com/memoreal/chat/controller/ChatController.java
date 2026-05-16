package com.memoreal.chat.controller;

import com.memoreal.chat.dto.ChatDto;
import com.memoreal.chat.service.ChatService;
import com.memoreal.domain.User;
import com.memoreal.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 새 채팅 세션 ID 발급
     * GET /api/chat/session
     */
    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, String>>> createSession() {
        String sessionId = chatService.createSession();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("sessionId", sessionId)));
    }

    /**
     * 음성 파일 업로드 → STT → GPT → 응답
     * POST /api/chat/voice
     *
     * 앱 사용 흐름:
     * 1. 어르신이 말씀하시는 동안 녹음
     * 2. 녹음 종료 후 이 API로 파일 전송
     * 3. STT 변환 + AI 응답 받기
     * 4. 채팅 화면에 sttText(말씀) + aiResponse(AI 답변) 표시
     */
    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<ChatDto.SttResponse>> processVoice(
            @AuthenticationPrincipal User user,
            @RequestParam String sessionId,
            @RequestParam("audio") MultipartFile audioFile) {

        log.info("음성 채팅 요청 - userId: {}, sessionId: {}, fileSize: {}bytes",
                user.getId(), sessionId, audioFile.getSize());

        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("음성 파일이 비어있습니다."));
        }

        ChatDto.SttResponse response = chatService.processVoice(user, sessionId, audioFile);
        return ResponseEntity.ok(ApiResponse.ok("음성 인식 완료", response));
    }

    /**
     * 텍스트 메시지 전송 (음성 없이 텍스트로 직접 입력 시)
     * POST /api/chat/message
     */
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatDto.ChatResponse>> sendMessage(
            @AuthenticationPrincipal User user,
            @RequestBody ChatDto.ChatRequest request) {

        log.info("텍스트 채팅 요청 - userId: {}, sessionId: {}", user.getId(), request.getSessionId());

        String aiResponse = chatService.processTextMessage(
                user, request.getSessionId(), request.getMessage()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                ChatDto.ChatResponse.builder()
                        .sessionId(request.getSessionId())
                        .role("assistant")
                        .content(aiResponse)
                        .isEnd(true)
                        .build()
        ));
    }

    /**
     * 세션 대화 이력 조회
     * GET /api/chat/history/{sessionId}
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<List<ChatDto.MessageHistory>>> getHistory(
            @AuthenticationPrincipal User user,
            @PathVariable String sessionId) {

        List<ChatDto.MessageHistory> history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
