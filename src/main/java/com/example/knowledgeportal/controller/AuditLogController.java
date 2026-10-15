package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.entity.AuditLog;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.security.PermissionService;
import com.example.knowledgeportal.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Accessing Enterprise Security Audit Logs.
 * Strictly restricted to administrators.
 */
@RestController
@RequestMapping("/api/knowledge/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final PermissionService permissionService;

    public AuditLogController(AuditLogService auditLogService, PermissionService permissionService) {
        this.auditLogService = auditLogService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(Authentication authentication) {
        if (!permissionService.isUserAdmin(authentication)) {
            throw new UnauthorizedAccessException("Viewing enterprise audit logs is restricted to administrators.");
        }
        return ResponseEntity.ok(auditLogService.getRecentAuditLogs());
    }
}
