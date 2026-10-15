package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DocumentVersion Entity
 * 
 * Preserves a snapshot of a document at a specific version point.
 * 
 * Beginner Concept:
 * In enterprise knowledge management, policies and procedures change over time.
 * Storing historical version snapshots allows:
 * 1. Auditing what was valid at a past date.
 * 2. Associating previous user questions and answers with the exact version consulted.
 * 3. Restoring or comparing changes between versions.
 */
@Entity
@Table(name = "document_versions", indexes = {
    @Index(name = "idx_doc_ver_lookup", columnList = "document_id, version_number")
})
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentCategory category;

    @Column(name = "custom_category", length = 100)
    private String customCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 50)
    private AccessLevel accessLevel;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DocumentVersion() {
    }

    public DocumentVersion(Long documentId, int versionNumber, String title,
                           DocumentCategory category, AccessLevel accessLevel,
                           String originalFileName, int chunkCount,
                           String changeSummary, String createdBy) {
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.title = title;
        this.category = category;
        this.accessLevel = accessLevel;
        this.originalFileName = originalFileName;
        this.chunkCount = chunkCount;
        this.changeSummary = changeSummary;
        this.createdBy = createdBy;
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

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
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

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
