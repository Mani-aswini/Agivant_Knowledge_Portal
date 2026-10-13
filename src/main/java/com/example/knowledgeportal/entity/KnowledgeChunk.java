package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * KnowledgeChunk Entity
 * 
 * Represents a segmented passage/slice of an uploaded document.
 * 
 * Beginner Concept:
 * When an employee asks a question or searches for keywords, searching through
 * whole 50-page documents returns too much noise. Segmenting the text into chunks
 * allows the system and AI agent to retrieve and cite the exact relevant paragraphs.
 * Every chunk is tied to a specific document ID and document version.
 */
@Entity
@Table(name = "knowledge_chunks", indexes = {
    @Index(name = "idx_chunk_doc_version", columnList = "document_id, document_version"),
    @Index(name = "idx_chunk_index", columnList = "chunk_index")
})
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_version", nullable = false)
    private int documentVersion;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "section_reference", length = 255)
    private String sectionReference;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "searchable_text", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String searchableText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public KnowledgeChunk() {
    }

    public KnowledgeChunk(Long documentId, int documentVersion, int chunkIndex,
                          String sectionReference, Integer pageNumber, String content) {
        this.documentId = documentId;
        this.documentVersion = documentVersion;
        this.chunkIndex = chunkIndex;
        this.sectionReference = sectionReference;
        this.pageNumber = pageNumber;
        this.content = content;
        this.searchableText = (content != null) ? content.toLowerCase() : "";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
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
        this.searchableText = (content != null) ? content.toLowerCase() : "";
    }

    public String getSearchableText() {
        return searchableText;
    }

    public void setSearchableText(String searchableText) {
        this.searchableText = searchableText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
