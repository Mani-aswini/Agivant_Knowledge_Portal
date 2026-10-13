package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "Search query cannot be blank.")
    private String query;

    private DocumentCategory category;
    private DocumentStatus status = DocumentStatus.ACTIVE;

    public SearchRequest() {
    }

    public SearchRequest(String query, DocumentCategory category, DocumentStatus status) {
        this.query = query;
        this.category = category;
        this.status = (status != null) ? status : DocumentStatus.ACTIVE;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }
}
