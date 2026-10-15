package com.example.knowledgeportal.service;

import com.example.knowledgeportal.entity.AuditLog;
import com.example.knowledgeportal.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing enterprise audit logging for security compliance.
 * 
 * Beginner Concept:
 * An audit log answers who did what, when, and with what outcome.
 * It is called across controllers and services whenever a document is
 * ingested, updated, re-indexed, archived, or restored.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordEvent(String actor, String action, String resourceType, Long resourceId, String outcome, String details) {
        try {
            AuditLog log = new AuditLog(actor, action, resourceType, resourceId, outcome, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Audit logging failures must not break core operations, but should be logged to console
            System.err.println("Warning: Failed to record audit log: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }
}
