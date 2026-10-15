package com.example.knowledgeportal.ai;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank(message = "Question cannot be blank.")
    private String question;

    private String conversationId;

    public AskRequest() {
    }

    public AskRequest(String question) {
        this.question = question;
    }

    public AskRequest(String question, String conversationId) {
        this.question = question;
        this.conversationId = conversationId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
