package com.vishnu.finance_tracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vishnu.finance_tracker.model.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop10BySessionIdOrderByCreatedAtDesc(Long sessionId);

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
}