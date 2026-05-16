package com.memoreal.chat.service;

import com.memoreal.chat.domain.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GptService {

    private final OpenAiService openAiService;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-tokens}")
    private Integer maxTokens;

    // Memoreal 자서전 인터뷰 시스템 프롬프트
    private static final String SYSTEM_PROMPT = """
            당신은 어르신의 인생 이야기를 따뜻하게 기록해드리는 자서전 인터뷰어 '작가'입니다.
            
            [역할]
            - 어르신의 소중한 기억과 경험을 자연스럽게 이끌어내는 인터뷰어
            - 어르신이 편안하게 이야기할 수 있도록 따뜻하고 공감적인 태도 유지
            - 짧고 명확한 질문으로 대화를 이어가기
            
            [대화 방식]
            - 존댓말 사용, 어르신을 '어르신'이라고 지칭
            - 어르신의 말에 공감과 감탄을 표현한 후 후속 질문
            - 한 번에 하나의 질문만 하기
            - 어르신이 말씀하신 키워드(장소, 사람, 사건)를 활용해 깊이 있는 질문으로 발전
            - 너무 어렵거나 불편한 주제는 자연스럽게 넘어가기
            
            [인터뷰 주제 (순서대로 진행)]
            1. 어린 시절과 고향
            2. 학창 시절과 친구들
            3. 사랑과 결혼
            4. 직업과 일
            5. 자녀와 가족
            6. 가장 행복했던 순간
            7. 힘들었던 시절과 극복
            8. 인생의 교훈과 후세에 전하고 싶은 말
            
            [시작]
            처음 만났을 때는 반갑게 인사하고 어린 시절부터 시작하세요.
            """;

    public GptService(@Value("${openai.api-key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
    }

    /**
     * 대화 이력을 포함해서 GPT에게 응답 요청
     *
     * @param sessionHistory 현재 세션의 대화 이력
     * @param userMessage    유저의 새 메시지
     * @return GPT 응답 텍스트
     */
    public String chat(List<ChatMessage> sessionHistory, String userMessage) {
        List<com.theokanning.openai.completion.chat.ChatMessage> messages = new ArrayList<>();

        // 1. 시스템 프롬프트
        messages.add(new com.theokanning.openai.completion.chat.ChatMessage(
                ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT
        ));

        // 2. 기존 대화 이력 (최근 20개로 제한 - 토큰 절약)
        int startIdx = Math.max(0, sessionHistory.size() - 20);
        for (ChatMessage msg : sessionHistory.subList(startIdx, sessionHistory.size())) {
            String role = msg.getRole() == ChatMessage.MessageRole.USER
                    ? ChatMessageRole.USER.value()
                    : ChatMessageRole.ASSISTANT.value();
            messages.add(new com.theokanning.openai.completion.chat.ChatMessage(role, msg.getContent()));
        }

        // 3. 새 유저 메시지
        messages.add(new com.theokanning.openai.completion.chat.ChatMessage(
                ChatMessageRole.USER.value(), userMessage
        ));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(0.8) // 자연스러운 대화를 위해 약간 높게
                .build();

        log.info("GPT 요청 - model: {}, messages: {}개", model, messages.size());

        String response = openAiService
                .createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

        log.info("GPT 응답: {}", response);
        return response;
    }
}
