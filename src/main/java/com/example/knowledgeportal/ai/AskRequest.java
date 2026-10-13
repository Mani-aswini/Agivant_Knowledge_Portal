package com.example.knowledgeportal.ai;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank(message = "Question cannot be blank.")
    private String question;

    public AskRequest() {
    }

    public AskRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
