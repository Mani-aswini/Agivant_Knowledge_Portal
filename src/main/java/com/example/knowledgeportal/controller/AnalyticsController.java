package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.dto.AnalyticsResponse;
import com.example.knowledgeportal.dto.DocumentResponse;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Dashboard and Analytics reporting.
 */
@RestController
@RequestMapping("/api/knowledge/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics(Authentication authentication) {
        return ResponseEntity.ok(analyticsService.getAnalyticsDashboard(authentication));
    }

    @GetMapping("/role-access")
    public ResponseEntity<List<DocumentResponse>> getRoleAccessibility(
            @RequestParam("role") UserRole role,
            Authentication authentication) {
        return ResponseEntity.ok(analyticsService.getDocumentsAccessibleToRole(role, authentication));
    }
}
