package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ChatMessage entity storing conversational history between users and the AI Assistant.
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_username", columnList = "username"),
        @Index(name = "idx_chat_created_at", columnList = "createdAt"),
        @Index(name = "idx_chat_conversation_id", columnList = "conversation_id")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(nullable = false, length = 20)
    private String role; // "user" or "assistant"

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sourcesJson;

    @Column(length = 100)
    private String answerReference;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(String username, String role, String content, String sourcesJson, String answerReference) {
        this(username, null, role, content, sourcesJson, answerReference);
    }

    public ChatMessage(String username, String conversationId, String role, String content, String sourcesJson, String answerReference) {
        this.username = username;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.sourcesJson = sourcesJson;
        this.answerReference = answerReference;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourcesJson() {
        return sourcesJson;
    }

    public void setSourcesJson(String sourcesJson) {
        this.sourcesJson = sourcesJson;
    }

    public String getAnswerReference() {
        return answerReference;
    }

    public void setAnswerReference(String answerReference) {
        this.answerReference = answerReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
