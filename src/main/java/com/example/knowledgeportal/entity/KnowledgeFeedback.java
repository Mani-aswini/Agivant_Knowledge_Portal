package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * KnowledgeFeedback Entity
 * 
 * Captures employee feedback and satisfaction ratings on answers provided by the system.
 */
@Entity
@Table(name = "knowledge_feedback", indexes = {
    @Index(name = "idx_feedback_doc", columnList = "document_id"),
    @Index(name = "idx_feedback_rating", columnList = "rating")
})
public class KnowledgeFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(name = "answer_reference", nullable = false, length = 100)
    private String answerReference;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public KnowledgeFeedback() {
    }

    public KnowledgeFeedback(Long documentId, String question, String answerReference,
                             int rating, String comment, String userId) {
        this(documentId, question, answerReference, rating, comment, userId, null);
    }

    public KnowledgeFeedback(Long documentId, String question, String answerReference,
                             int rating, String comment, String userId, String answerText) {
        this.documentId = documentId;
        this.question = question;
        this.answerReference = answerReference;
        this.rating = rating;
        this.comment = comment;
        this.userId = userId;
        this.answerText = answerText;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswerReference() {
        return answerReference;
    }

    public void setAnswerReference(String answerReference) {
        this.answerReference = answerReference;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
