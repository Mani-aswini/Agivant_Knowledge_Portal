package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.KnowledgeFeedback;

import java.time.LocalDateTime;

public class FeedbackResponse {

    private Long id;
    private Long documentId;
    private String question;
    private String answerReference;
    private String answerText;
    private int rating;
    private String comment;
    private String userId;
    private LocalDateTime createdAt;
    private String message;

    public FeedbackResponse() {
    }

    public FeedbackResponse(KnowledgeFeedback feedback, String message) {
        this.id = feedback.getId();
        this.documentId = feedback.getDocumentId();
        this.question = feedback.getQuestion();
        this.answerReference = feedback.getAnswerReference();
        this.answerText = feedback.getAnswerText();
        this.rating = feedback.getRating();
        this.comment = feedback.getComment();
        this.userId = feedback.getUserId();
        this.createdAt = feedback.getCreatedAt();
        this.message = message;
    }

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

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
