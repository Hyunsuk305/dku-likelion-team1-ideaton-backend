package com.memoreal.chat.repository;

import com.memoreal.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 세션의 전체 대화 이력 (GPT에게 context로 넘길 때 사용)
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // 세션의 최근 N개 대화 (컨텍스트 길이 제한)
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(String sessionId);

    // 유저의 전체 세션 목록
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
}
