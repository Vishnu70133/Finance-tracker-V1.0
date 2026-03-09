package com.vishnu.finance_tracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vishnu.finance_tracker.model.ChatSession;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<ChatSession> findByUserEmailOrderByUpdatedAtDesc(String email);
}