package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.entity.KnowledgeDocument;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String title;
    private String description;
    private DocumentCategory category;
    private String owner;
    private AccessLevel accessLevel;
    private int version;
    private DocumentStatus status;
    private String originalFileName;
    private String contentType;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private boolean needsReindex;
    private LocalDateTime archivedAt;
    private int chunkCount;

    public DocumentResponse() {
    }

    public DocumentResponse(KnowledgeDocument doc, int chunkCount) {
        this.id = doc.getId();
        this.title = doc.getTitle();
        this.description = doc.getDescription();
        this.category = doc.getCategory();
        this.owner = doc.getOwner();
        this.accessLevel = doc.getAccessLevel();
        this.version = doc.getVersion();
        this.status = doc.getStatus();
        this.originalFileName = doc.getOriginalFileName();
        this.contentType = doc.getContentType();
        this.uploadedAt = doc.getUploadedAt();
        this.updatedAt = doc.getUpdatedAt();
        this.needsReindex = doc.isNeedsReindex();
        this.archivedAt = doc.getArchivedAt();
        this.chunkCount = chunkCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isNeedsReindex() {
        return needsReindex;
    }

    public void setNeedsReindex(boolean needsReindex) {
        this.needsReindex = needsReindex;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }
}
