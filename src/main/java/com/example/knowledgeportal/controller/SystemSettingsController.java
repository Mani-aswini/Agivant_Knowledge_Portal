package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.ai.AiClient;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller providing verified System Configuration & Status.
 * Strictly restricted to administrators. Credentials are never exposed.
 */
@RestController
@RequestMapping("/api/knowledge/system-settings")
public class SystemSettingsController {

    private final AiClient aiClient;
    private final PermissionService permissionService;

    @Value("${knowledge.upload.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    @Value("${knowledge.upload.dir:./uploads}")
    private String uploadDirectory;

    public SystemSettingsController(AiClient aiClient, PermissionService permissionService) {
        this.aiClient = aiClient;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemSettings(Authentication authentication) {
        if (!permissionService.isUserAdmin(authentication)) {
            throw new UnauthorizedAccessException("Viewing system settings is restricted to administrators.");
        }

        Map<String, Object> settings = new LinkedHashMap<>();

        // 1. AI Orchestration Settings (credentials masked/never exposed)
        boolean isLiveAiConfigured = aiClient.isConfigured();
        settings.put("aiProvider", isLiveAiConfigured ? "Google Gemini (" + aiClient.getProvider() + ")" : "Simulated Prototype Engine (Deterministic RAG)");
        settings.put("aiModelName", isLiveAiConfigured ? aiClient.getModelName() : "Extractive Sentence Synthesis Engine");
        settings.put("executionMode", isLiveAiConfigured ? "REAL_AI" : "SIMULATION");
        settings.put("aiApiKeyConfigured", isLiveAiConfigured);


        // 2. Retrieval & Search Engine
        settings.put("retrievalMode", "Keyword-Based Scoring with Section & Title Weighting");
        settings.put("retrievalDetails", "Exact match: +10, Title match: +5, Section match: +3, Token match: +2");
        settings.put("embeddingFallback", "Active (Keyword Scoring Engine)");

        // 3. Document Ingestion Limits
        settings.put("supportedFormats", List.of("Plain Text (.txt)", "Adobe PDF (.pdf)"));
        settings.put("maxFileSizeBytes", maxUploadSizeBytes);
        settings.put("maxFileSizeDisplay", (maxUploadSizeBytes / (1024 * 1024)) + " MB");
        settings.put("uploadDirectory", uploadDirectory);

        // 4. Security & Role Configuration
        settings.put("rolesSupported", List.of("ROLE_ADMIN", "ROLE_EMPLOYEE", "ROLE_HR"));
        settings.put("accessLevelsSupported", List.of("PUBLIC", "EMPLOYEE", "HR", "ADMIN"));
        settings.put("promptInjectionDefense", "Active (instructional pattern scanning and neutralization)");

        return ResponseEntity.ok(settings);
    }
}
