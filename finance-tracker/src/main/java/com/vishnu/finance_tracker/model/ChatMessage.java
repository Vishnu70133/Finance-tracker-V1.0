package com.vishnu.finance_tracker.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;

   @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonBackReference
    private ChatSession session;

    public ChatMessage() {}

    public ChatMessage(String role, String message, ChatSession session) {
        this.role = role;
        this.message = message;
        this.session = session;
        this.createdAt = LocalDateTime.now();
    }

    /* GETTERS */

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public ChatSession getSession() {
        return session;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /* SETTERS */

    public void setRole(String role) {
        this.role = role;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}