package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.DocumentCategory;

public class SearchResultItem {

    private Long documentId;
    private String title;
    private DocumentCategory category;
    private String customCategory;
    private int version;
    private Long chunkId;
    private int chunkIndex;
    private String sectionReference;
    private Integer pageNumber;
    private String snippet;
    private double score;
    private String contentType;
    private String originalFileName;
    private String description;

    public SearchResultItem() {
    }

    public SearchResultItem(Long documentId, String title, DocumentCategory category,
                            int version, Long chunkId, int chunkIndex,
                            String sectionReference, Integer pageNumber,
                            String snippet, double score) {
        this.documentId = documentId;
        this.title = title;
        this.category = category;
        this.version = version;
        this.chunkId = chunkId;
        this.chunkIndex = chunkIndex;
        this.sectionReference = sectionReference;
        this.pageNumber = pageNumber;
        this.snippet = snippet;
        this.score = score;
    }

    // Getters and Setters
    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
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

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
