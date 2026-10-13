package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.dto.DocumentDetailResponse;
import com.example.knowledgeportal.dto.DocumentResponse;
import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.service.KnowledgeDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for Knowledge Document Lifecycle Management.
 */
@RestController
@RequestMapping("/api/knowledge/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload and index a new document.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("category") DocumentCategory category,
            @RequestParam("accessLevel") AccessLevel accessLevel,
            Authentication authentication) {

        DocumentResponse response = documentService.uploadAndIndexDocument(
                file, title, description, category, accessLevel, authentication
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all documents accessible to the current user with optional filters.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @RequestParam(value = "category", required = false) DocumentCategory category,
            @RequestParam(value = "status", required = false) DocumentStatus status,
            @RequestParam(value = "accessLevel", required = false) AccessLevel accessLevel,
            Authentication authentication) {

        List<DocumentResponse> documents = documentService.listAccessibleDocuments(
                category, status, accessLevel, authentication
        );
        return ResponseEntity.ok(documents);
    }

    /**
     * Retrieve document details and current version chunks.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetails(
            @PathVariable("id") Long id,
            Authentication authentication) {

        DocumentDetailResponse response = documentService.getDocumentDetails(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Re-index an existing document.
     */
    @PutMapping("/{id}/reindex")
    public ResponseEntity<DocumentResponse> reindexDocument(
            @PathVariable("id") Long id,
            Authentication authentication) {

        DocumentResponse response = documentService.reindexDocument(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Safely archive a document (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DocumentResponse> archiveDocument(
            @PathVariable("id") Long id,
            Authentication authentication) {

        DocumentResponse response = documentService.archiveDocument(id, authentication);
        return ResponseEntity.ok(response);
    }
}
