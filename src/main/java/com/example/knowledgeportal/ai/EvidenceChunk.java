package com.example.knowledgeportal.ai;

/**
 * Structured evidence item retrieved by the agent tools for fact verification.
 */
public class EvidenceChunk {

    private Long documentId;
    private String documentTitle;
    private int documentVersion;
    private Long chunkId;
    private int chunkIndex;
    private String sectionReference;
    private Integer pageNumber;
    private String content;
    private double relevanceScore;

    public EvidenceChunk() {
    }

    public EvidenceChunk(Long documentId, String documentTitle, int documentVersion,
                         Long chunkId, int chunkIndex, String sectionReference,
                         Integer pageNumber, String content, double relevanceScore) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.documentVersion = documentVersion;
        this.chunkId = chunkId;
        this.chunkIndex = chunkIndex;
        this.sectionReference = sectionReference;
        this.pageNumber = pageNumber;
        this.content = content;
        this.relevanceScore = relevanceScore;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public int getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(int documentVersion) {
        this.documentVersion = documentVersion;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}
