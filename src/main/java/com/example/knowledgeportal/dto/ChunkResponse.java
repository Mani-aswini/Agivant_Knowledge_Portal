package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.KnowledgeChunk;

import java.time.LocalDateTime;

public class ChunkResponse {

    private Long id;
    private Long documentId;
    private int documentVersion;
    private int chunkIndex;
    private String sectionReference;
    private Integer pageNumber;
    private String content;
    private LocalDateTime createdAt;

    public ChunkResponse() {
    }

    public ChunkResponse(KnowledgeChunk chunk) {
        this.id = chunk.getId();
        this.documentId = chunk.getDocumentId();
        this.documentVersion = chunk.getDocumentVersion();
        this.chunkIndex = chunk.getChunkIndex();
        this.sectionReference = chunk.getSectionReference();
        this.pageNumber = chunk.getPageNumber();
        this.content = chunk.getContent();
        this.createdAt = chunk.getCreatedAt();
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

    public int getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(int documentVersion) {
        this.documentVersion = documentVersion;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getSectionReference() {
        return sectionReference;
    }

    public void setSectionReference(String sectionReference) {
        this.sectionReference = sectionReference;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
