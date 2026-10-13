package com.example.knowledgeportal.ai;

import java.util.List;

public class AskResponse {

    private String question;
    private String answer;
    private boolean abstained;
    private String mode;
    private List<CitationSource> sources;
    private String reasoningNotes;

    public AskResponse() {
    }

    public AskResponse(String question, String answer, boolean abstained, String mode,
                       List<CitationSource> sources, String reasoningNotes) {
        this.question = question;
        this.answer = answer;
        this.abstained = abstained;
        this.mode = mode;
        this.sources = sources;
        this.reasoningNotes = reasoningNotes;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isAbstained() {
        return abstained;
    }

    public void setAbstained(boolean abstained) {
        this.abstained = abstained;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<CitationSource> getSources() {
        return sources;
    }

    public void setSources(List<CitationSource> sources) {
        this.sources = sources;
    }

    public String getReasoningNotes() {
        return reasoningNotes;
    }

    public void setReasoningNotes(String reasoningNotes) {
        this.reasoningNotes = reasoningNotes;
    }
}
