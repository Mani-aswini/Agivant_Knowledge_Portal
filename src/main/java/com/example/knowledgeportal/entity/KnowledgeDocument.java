package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * KnowledgeDocument Entity
 * 
 * Represents an approved enterprise document uploaded to the knowledge portal.
 * 
 * Beginner Concept:
 * Each document contains metadata (title, category, owner, access level)
 * and holds a monotonically increasing version number. When a document is
 * re-indexed, its version is incremented so obsolete chunks can be excluded.
 */
@Entity
@Table(name = "knowledge_documents", indexes = {
    @Index(name = "idx_doc_category", columnList = "category"),
    @Index(name = "idx_doc_status", columnList = "status"),
    @Index(name = "idx_doc_access", columnList = "access_level"),
    @Index(name = "idx_doc_title", columnList = "title")
})
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentCategory category;

    @Column(nullable = false, length = 100)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 50)
    private AccessLevel accessLevel;

    @Column(nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "searchable_text", columnDefinition = "LONGTEXT")
    private String searchableText;

    @Column(name = "file_storage_path", length = 500)
    private String fileStoragePath;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "needs_reindex", nullable = false)
    private boolean needsReindex = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public KnowledgeDocument() {
    }

    public KnowledgeDocument(String title, String description, DocumentCategory category,
                             String owner, AccessLevel accessLevel, String originalFileName,
                             String contentType, String searchableText, String fileStoragePath) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.owner = owner;
        this.accessLevel = accessLevel;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.searchableText = searchableText;
        this.fileStoragePath = fileStoragePath;
        this.version = 1;
        this.status = DocumentStatus.ACTIVE;
        this.needsReindex = false;
        this.uploadedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to safely archive a document
    public void archive() {
        this.status = DocumentStatus.ARCHIVED;
        this.archivedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method for version increment upon re-indexing
    public void incrementVersion() {
        this.version += 1;
        this.updatedAt = LocalDateTime.now();
        this.needsReindex = false;
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

    public String getSearchableText() {
        return searchableText;
    }

    public void setSearchableText(String searchableText) {
        this.searchableText = searchableText;
    }

    public String getFileStoragePath() {
        return fileStoragePath;
    }

    public void setFileStoragePath(String fileStoragePath) {
        this.fileStoragePath = fileStoragePath;
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
}
