package com.vishnu.finance_tracker.model;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String userEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;  

    // Optional: relation to messages (useful later)
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonManagedReference
    private List<ChatMessage> messages;

    // Default constructor required by JPA
    public ChatSession() {}

    // Constructor for creating new session
    public ChatSession(String userEmail) {
        this.userEmail = userEmail;
        this.createdAt = LocalDateTime.now();
         this.updatedAt = LocalDateTime.now(); 
        this.title = "New Chat";
    }

    /* ========================
       GETTERS
       ======================== */

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /* ========================
       SETTERS
       ======================== */

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}