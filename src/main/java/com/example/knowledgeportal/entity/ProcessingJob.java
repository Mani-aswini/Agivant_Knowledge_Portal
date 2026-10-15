package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ProcessingJob Entity
 * 
 * Tracks the lifecycle and execution state of document processing tasks
 * (text extraction, chunking, vector indexing) with error reporting and retry count.
 * 
 * Beginner Concept:
 * In enterprise search, document ingestion is an operational pipeline.
 * By recording processing jobs in the database, administrators can see:
 * - Pending jobs
 * - Failed jobs with their exact error reason
 * - Trigger a safe retry action to re-attempt failed jobs
 */
@Entity
@Table(name = "processing_jobs", indexes = {
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_doc", columnList = "document_id")
})
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "document_title", nullable = false, length = 255)
    private String documentTitle;

    @Column(name = "document_version", nullable = false)
    private int documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IndexingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public ProcessingJob() {
    }

    public ProcessingJob(Long documentId, String documentTitle, int documentVersion,
                         JobType jobType, IndexingStatus status) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.documentVersion = documentVersion;
        this.jobType = jobType;
        this.status = status;
        this.retryCount = 0;
        this.startedAt = LocalDateTime.now();
        if (status == IndexingStatus.INDEXED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void markSuccess() {
        this.status = IndexingStatus.INDEXED;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailure(String error) {
        this.status = IndexingStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount += 1;
        this.status = IndexingStatus.PENDING;
        this.startedAt = LocalDateTime.now();
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

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public IndexingStatus getStatus() {
        return status;
    }

    public void setStatus(IndexingStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
