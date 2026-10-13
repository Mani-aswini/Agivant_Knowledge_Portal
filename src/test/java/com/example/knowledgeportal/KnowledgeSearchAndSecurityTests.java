package com.example.knowledgeportal;

import com.example.knowledgeportal.dto.*;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.service.FeedbackService;
import com.example.knowledgeportal.service.KnowledgeDocumentService;
import com.example.knowledgeportal.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class KnowledgeSearchAndSecurityTests {

    @Autowired
    private SearchService searchService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private KnowledgeDocumentService documentService;

    private Authentication createAuth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "pass", List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    @DisplayName("TC-05-06: Search with metadata filters and verify that only matching, accessible results are returned")
    void testSearchWithMetadataFilters() {
        Authentication employeeAuth = createAuth("employee1", "ROLE_EMPLOYEE");

        // 1. Search with category filter POLICY
        SearchResponse policyRes = searchService.search(
                new SearchRequest("leave", DocumentCategory.POLICY, DocumentStatus.ACTIVE), employeeAuth
        );
        assertNotNull(policyRes);
        for (SearchResultItem item : policyRes.getResults()) {
            assertEquals(DocumentCategory.POLICY, item.getCategory(), "Must match filtered category POLICY.");
        }

        // 2. Search with category filter TECHNICAL_RUNBOOK
        SearchResponse runbookRes = searchService.search(
                new SearchRequest("incident", DocumentCategory.TECHNICAL_RUNBOOK, DocumentStatus.ACTIVE), employeeAuth
        );
        assertNotNull(runbookRes);
        for (SearchResultItem item : runbookRes.getResults()) {
            assertEquals(DocumentCategory.TECHNICAL_RUNBOOK, item.getCategory(), "Must match filtered category TECHNICAL_RUNBOOK.");
        }
    }

    @Test
    @DisplayName("TC-05-07: Search / Ask about restricted content as an unauthorized user and verify no restricted text is leaked")
    void testRestrictedContentNotLeakedToUnauthorizedUser() {
        Authentication employeeAuth = createAuth("employee1", "ROLE_EMPLOYEE");

        // 1. Query keywords specific to Seed Document 3 (HR Executive Compensation - AccessLevel: HR)
        SearchResponse hrSearch = searchService.search(
                new SearchRequest("EBITDA corporate benchmarks bonus tiers", null, DocumentStatus.ACTIVE), employeeAuth
        );

        // Employee MUST NOT receive any chunks from HR restricted documents
        assertEquals(0, hrSearch.getTotalResults(), "ROLE_EMPLOYEE must not receive chunks for HR-restricted query.");
        assertTrue(hrSearch.getResults().isEmpty());

        // 2. Query keywords specific to Seed Document 4 (Admin Infrastructure - AccessLevel: ADMIN)
        SearchResponse adminSearch = searchService.search(
                new SearchRequest("Super-admin bastion access FIDO2 token", null, DocumentStatus.ACTIVE), employeeAuth
        );
        assertEquals(0, adminSearch.getTotalResults(), "ROLE_EMPLOYEE must not receive chunks from ADMIN restricted documents.");
        assertTrue(adminSearch.getResults().isEmpty());

        // 3. Search query that matches both public/employee docs and restricted docs:
        // Verify that NO result item belongs to restricted Document 3 or Document 4
        SearchResponse broadSearch = searchService.search(
                new SearchRequest("annual performance policy bonus bastion", null, DocumentStatus.ACTIVE), employeeAuth
        );
        boolean containsRestrictedDoc = broadSearch.getResults().stream()
                .anyMatch(r -> r.getDocumentId().equals(3L) || r.getDocumentId().equals(4L));
        assertFalse(containsRestrictedDoc, "Search results must never contain Document 3 (HR) or Document 4 (ADMIN) for an employee.");

        // 4. Also test direct chunk / document details unauthorized attempt (Doc 3 ID = 3, Doc 4 ID = 4)
        assertThrows(UnauthorizedAccessException.class, () -> {
            documentService.getDocumentDetails(3L, employeeAuth);
        }, "Directly requesting details of HR restricted document must throw UnauthorizedAccessException.");

        assertThrows(UnauthorizedAccessException.class, () -> {
            documentService.getDocumentDetails(4L, employeeAuth);
        }, "Directly requesting details of ADMIN restricted document must throw UnauthorizedAccessException.");
    }

    @Test
    @Transactional
    @DisplayName("TC-05-11: Submit feedback and verify that it is persisted and associated with the correct document and answer reference")
    void testSubmitFeedbackPersistenceAndValidation() {
        Authentication employeeAuth = createAuth("employee1", "ROLE_EMPLOYEE");

        // 1. Valid feedback submission for accessible document (Doc 1: Annual Leave)
        FeedbackRequest validReq = new FeedbackRequest(
                1L,
                "How many days of annual leave are available?",
                "ANS-POLICY-LEAVE-01",
                5,
                "Clear, factual answer with correct section citation."
        );
        FeedbackResponse res = feedbackService.submitFeedback(validReq, employeeAuth);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals(1L, res.getDocumentId());
        assertEquals(5, res.getRating());
        assertEquals("employee1", res.getUserId());
        assertEquals("ANS-POLICY-LEAVE-01", res.getAnswerReference());

        // 2. Reject invalid rating (< 1 or > 5)
        FeedbackRequest invalidLow = new FeedbackRequest(1L, "Question?", "REF-1", 0, "Too low rating");
        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(invalidLow, employeeAuth));

        FeedbackRequest invalidHigh = new FeedbackRequest(1L, "Question?", "REF-2", 6, "Too high rating");
        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(invalidHigh, employeeAuth));

        // 3. Reject feedback on non-existent document
        FeedbackRequest missingDoc = new FeedbackRequest(9999L, "Question?", "REF-3", 4, "Missing doc");
        assertThrows(ResourceNotFoundException.class, () -> feedbackService.submitFeedback(missingDoc, employeeAuth));

        // 4. Reject feedback on document unauthorized to the user (Doc 4 is ADMIN only)
        FeedbackRequest unauthorizedDoc = new FeedbackRequest(4L, "Question?", "REF-4", 4, "Unauthorized doc");
        assertThrows(UnauthorizedAccessException.class, () -> feedbackService.submitFeedback(unauthorizedDoc, employeeAuth));
    }
}
