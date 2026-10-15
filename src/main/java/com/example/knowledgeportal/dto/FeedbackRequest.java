package com.example.knowledgeportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    private Long documentId;

    @NotBlank(message = "Question is mandatory")
    private String question;

    @NotBlank(message = "Answer reference is mandatory")
    private String answerReference;

    private String answerText;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private int rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    public FeedbackRequest() {
    }

    public FeedbackRequest(Long documentId, String question, String answerReference, int rating, String comment) {
        this(documentId, question, answerReference, rating, comment, null);
    }

    public FeedbackRequest(Long documentId, String question, String answerReference, int rating, String comment, String answerText) {
        this.documentId = documentId;
        this.question = question;
        this.answerReference = answerReference;
        this.rating = rating;
        this.comment = comment;
        this.answerText = answerText;
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

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
