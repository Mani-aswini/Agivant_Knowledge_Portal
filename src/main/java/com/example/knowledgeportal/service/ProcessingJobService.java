package com.example.knowledgeportal.service;

import com.example.knowledgeportal.entity.IndexingStatus;
import com.example.knowledgeportal.entity.JobType;
import com.example.knowledgeportal.entity.KnowledgeDocument;
import com.example.knowledgeportal.entity.ProcessingJob;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.repository.ProcessingJobRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service managing document processing pipeline health and retry operations.
 * 
 * Beginner Concept:
 * In a real production system, indexing jobs run through parsers and vectorizers.
 * Tracking each job's start time, completion time, status, and error message
 * enables the Document Processing Health dashboard card to show true operational state
 * and allows administrators to safely retry failed jobs.
 */
@Service
public class ProcessingJobService {

    private final ProcessingJobRepository jobRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentService documentService;
    private final AuditLogService auditLogService;

    public ProcessingJobService(ProcessingJobRepository jobRepository,
                                KnowledgeDocumentRepository documentRepository,
                                @Lazy KnowledgeDocumentService documentService,
                                AuditLogService auditLogService) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.auditLogService = auditLogService;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public ProcessingJob recordJobStart(Long documentId, String title, int version, JobType jobType) {
        ProcessingJob job = new ProcessingJob(documentId, title, version, jobType, IndexingStatus.PENDING);
        return jobRepository.save(job);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordJobSuccess(Long jobId) {
        if (jobId == null) return;
        jobRepository.findById(jobId).ifPresent(job -> {
            job.markSuccess();
            jobRepository.save(job);
        });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordJobFailure(Long jobId, String errorMessage) {
        if (jobId == null) return;
        jobRepository.findById(jobId).ifPresent(job -> {
            job.markFailure(errorMessage);
            jobRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public List<ProcessingJob> getRecentJobs() {
        return jobRepository.findTop50ByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProcessingHealthSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        long failedCount = jobRepository.countByStatus(IndexingStatus.FAILED);
        long pendingCount = jobRepository.countByStatus(IndexingStatus.PENDING);
        long indexedCount = documentRepository.countByIndexingStatus(IndexingStatus.INDEXED);

        String operationalStatus = (failedCount > 0) ? "DEGRADED" : (pendingCount > 0 ? "PROCESSING" : "OPERATIONAL");

        // Find last successful job timestamp
        List<ProcessingJob> recent = jobRepository.findTop50ByOrderByStartedAtDesc();
        LocalDateTime lastSuccess = recent.stream()
                .filter(j -> j.getStatus() == IndexingStatus.INDEXED && j.getCompletedAt() != null)
                .map(ProcessingJob::getCompletedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        summary.put("operationalStatus", operationalStatus);
        summary.put("indexedDocumentCount", indexedCount);
        summary.put("pendingJobs", pendingCount);
        summary.put("failedJobs", failedCount);
        summary.put("lastSuccessTime", lastSuccess.toString());
        summary.put("searchAvailability", "AVAILABLE");
        summary.put("keywordFallbackAvailable", true);

        return summary;
    }

    /**
     * Retries a failed processing job by re-triggering indexing on its document.
     */
    @Transactional
    public ProcessingJob retryJob(Long jobId, Authentication auth) {
        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Processing job not found with ID: " + jobId));

        job.incrementRetry();
        job = jobRepository.save(job);

        try {
            documentService.reindexDocument(job.getDocumentId(), auth);
            job.markSuccess();
            job = jobRepository.save(job);

            auditLogService.recordEvent(
                    (auth != null ? auth.getName() : "SYSTEM"),
                    "RETRY_PROCESSING_JOB",
                    "PROCESSING_JOB",
                    job.getId(),
                    "SUCCESS",
                    "Successfully retried processing job for document: " + job.getDocumentTitle()
            );
        } catch (Exception e) {
            job.markFailure(e.getMessage());
            job = jobRepository.save(job);

            auditLogService.recordEvent(
                    (auth != null ? auth.getName() : "SYSTEM"),
                    "RETRY_PROCESSING_JOB",
                    "PROCESSING_JOB",
                    job.getId(),
                    "FAILURE",
                    "Failed retrying job: " + e.getMessage()
            );
        }

        return job;
    }
}
