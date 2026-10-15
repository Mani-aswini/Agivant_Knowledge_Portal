package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.entity.ProcessingJob;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.security.PermissionService;
import com.example.knowledgeportal.service.ProcessingJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Document Processing Pipeline Jobs and Retries.
 * Strictly restricted to administrators.
 */
@RestController
@RequestMapping("/api/knowledge/processing-jobs")
public class ProcessingJobController {

    private final ProcessingJobService processingJobService;
    private final PermissionService permissionService;

    public ProcessingJobController(ProcessingJobService processingJobService,
                                   PermissionService permissionService) {
        this.processingJobService = processingJobService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<ProcessingJob>> getProcessingJobs(Authentication authentication) {
        if (!permissionService.isUserAdmin(authentication)) {
            throw new UnauthorizedAccessException("Viewing document processing jobs is restricted to administrators.");
        }
        return ResponseEntity.ok(processingJobService.getRecentJobs());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getProcessingHealth(Authentication authentication) {
        return ResponseEntity.ok(processingJobService.getProcessingHealthSummary());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ProcessingJob> retryJob(@PathVariable("id") Long id,
                                                  Authentication authentication) {
        if (!permissionService.isUserAdmin(authentication)) {
            throw new UnauthorizedAccessException("Retrying document processing jobs is restricted to administrators.");
        }
        ProcessingJob retried = processingJobService.retryJob(id, authentication);
        return ResponseEntity.ok(retried);
    }
}
