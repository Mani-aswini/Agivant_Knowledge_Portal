package com.example.knowledgeportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FeedbackRequest {

    @NotNull(message = "Document ID is mandatory")
    private Long documentId;

    @NotBlank(message = "Question is mandatory")
    private String question;

    @NotBlank(message = "Answer reference is mandatory")
    private String answerReference;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private int rating;

    private String comment;

    public FeedbackRequest() {
    }

    public FeedbackRequest(Long documentId, String question, String answerReference, int rating, String comment) {
        this.documentId = documentId;
        this.question = question;
        this.answerReference = answerReference;
        this.rating = rating;
        this.comment = comment;
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
}
