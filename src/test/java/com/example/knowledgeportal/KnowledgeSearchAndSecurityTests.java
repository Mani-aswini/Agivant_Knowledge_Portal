package com.example.knowledgeportal;

import com.example.knowledgeportal.dto.*;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.service.AnalyticsService;
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

    @Autowired
    private AnalyticsService analyticsService;

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

        // 5. Submit feedback without document (Direct answer or abstention, docId = null)
        FeedbackRequest abstentionFeedback = new FeedbackRequest(
                null,
                "What is the company stock purchase plan discount?",
                "ANS-ABSTAIN-001",
                4,
                "Good honest abstention when evidence was not found."
        );
        abstentionFeedback.setAnswerText("I cannot find sufficient evidence in the approved enterprise documents.");
        FeedbackResponse abstainRes = feedbackService.submitFeedback(abstentionFeedback, employeeAuth);
        assertNotNull(abstainRes);
        assertNull(abstainRes.getDocumentId());
        assertEquals("ANS-ABSTAIN-001", abstainRes.getAnswerReference());
        assertEquals("I cannot find sufficient evidence in the approved enterprise documents.", abstainRes.getAnswerText());

        // 6. Comment length exceeding 1000 characters is rejected
        String longComment = "a".repeat(1001);
        FeedbackRequest tooLongReq = new FeedbackRequest(1L, "Question?", "REF-5", 5, longComment);
        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(tooLongReq, employeeAuth));

        // 7. Blank question or answer reference is rejected
        FeedbackRequest blankQReq = new FeedbackRequest(1L, "   ", "REF-6", 5, "Comment");
        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(blankQReq, employeeAuth));

        FeedbackRequest blankRefReq = new FeedbackRequest(1L, "Question?", "  ", 5, "Comment");
        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(blankRefReq, employeeAuth));
    }

    @Test
    @Transactional
    @DisplayName("TC-05-12: Administrative feedback review and analytics with role-based security isolation")
    void testFeedbackManagementAnalyticsAndSecurity() {
        Authentication employeeAuth = createAuth("employee1", "ROLE_EMPLOYEE");
        Authentication adminAuth = createAuth("admin_user", "ROLE_ADMIN");
        Authentication hrAuth = createAuth("hr_user", "ROLE_HR");

        // 1. Submit sample feedbacks from employee
        FeedbackRequest fb1 = new FeedbackRequest(1L, "How do I request annual leave?", "ANS-REF-101", 5, "Excellent and accurate");
        fb1.setAnswerText("Submit request in Workday portal 2 weeks in advance.");
        feedbackService.submitFeedback(fb1, employeeAuth);

        FeedbackRequest fb2 = new FeedbackRequest(1L, "What is the rollover policy?", "ANS-REF-102", 3, "Decent answer but missing carryover cap");
        fb2.setAnswerText("Unused leave can be carried over.");
        feedbackService.submitFeedback(fb2, employeeAuth);

        FeedbackRequest fb3 = new FeedbackRequest(null, "How do I expense travel dinners?", "ANS-REF-103", 4, "Helpful abstention advice");
        fb3.setAnswerText("Travel dinner policy requires receipts over $25.");
        feedbackService.submitFeedback(fb3, employeeAuth);

        // 2. Security: Unauthorized employee cannot access feedback analytics
        assertThrows(UnauthorizedAccessException.class, () -> {
            feedbackService.getFeedbackManagement(null, null, employeeAuth);
        }, "ROLE_EMPLOYEE must be denied access to feedback management analytics.");

        // 3. Security: Admin and HR can view feedback management analytics
        FeedbackManagementResponse adminView = feedbackService.getFeedbackManagement(null, null, adminAuth);
        assertNotNull(adminView);
        assertTrue(adminView.getTotalSubmissions() >= 3);
        assertTrue(adminView.isHasRatings());
        assertTrue(adminView.getAverageRating() > 0.0);
        assertNotNull(adminView.getRatingDistribution());
        assertTrue(adminView.getRatingDistribution().get(5) >= 1);
        assertTrue(adminView.getRatingDistribution().get(3) >= 1);
        assertTrue(adminView.getRatingDistribution().get(4) >= 1);

        // Verify HR also has access
        FeedbackManagementResponse hrView = feedbackService.getFeedbackManagement(null, null, hrAuth);
        assertNotNull(hrView);
        assertEquals(adminView.getTotalSubmissions(), hrView.getTotalSubmissions());

        // 4. Verify enriched document details in items
        FeedbackAdminItemResponse docItem = adminView.getItems().stream()
                .filter(i -> "ANS-REF-101".equals(i.getAnswerReference()))
                .findFirst()
                .orElse(null);
        assertNotNull(docItem);
        assertEquals(1L, docItem.getDocumentId());
        assertNotNull(docItem.getDocumentTitle());
        assertEquals("employee1", docItem.getUserId());
        assertEquals("Submit request in Workday portal 2 weeks in advance.", docItem.getAnswerText());

        // Verify item without document
        FeedbackAdminItemResponse noDocItem = adminView.getItems().stream()
                .filter(i -> "ANS-REF-103".equals(i.getAnswerReference()))
                .findFirst()
                .orElse(null);
        assertNotNull(noDocItem);
        assertNull(noDocItem.getDocumentId());
        assertEquals("Travel dinner policy requires receipts over $25.", noDocItem.getAnswerText());

        // 5. Test rating filter (filter for 5 stars only)
        FeedbackManagementResponse fiveStarOnly = feedbackService.getFeedbackManagement(5, null, adminAuth);
        assertNotNull(fiveStarOnly);
        for (FeedbackAdminItemResponse item : fiveStarOnly.getItems()) {
            assertEquals(5, item.getRating(), "All filtered items must have rating 5.");
        }

        // 6. Test search filter (search for keyword 'rollover')
        FeedbackManagementResponse searchRes = feedbackService.getFeedbackManagement(null, "rollover", adminAuth);
        assertNotNull(searchRes);
        assertFalse(searchRes.getItems().isEmpty());
        assertTrue(searchRes.getItems().stream().anyMatch(i -> i.getQuestion().contains("rollover") || i.getComment().contains("carryover")));
    }

    @Test
    @DisplayName("TC-05-11: Dashboard analytics active document count is strictly role-scoped based on user clearance")
    void testRoleScopedAnalyticsDocumentCounts() {
        Authentication employeeAuth = createAuth("employee1", "ROLE_EMPLOYEE");
        Authentication hrAuth = createAuth("hr_user", "ROLE_HR");
        Authentication adminAuth = createAuth("admin_user", "ROLE_ADMIN");

        AnalyticsResponse employeeDashboard = analyticsService.getAnalyticsDashboard(employeeAuth);
        AnalyticsResponse hrDashboard = analyticsService.getAnalyticsDashboard(hrAuth);
        AnalyticsResponse adminDashboard = analyticsService.getAnalyticsDashboard(adminAuth);

        // In seeded test data:
        // Doc 1 (EMPLOYEE), Doc 2 (EMPLOYEE), Doc 5 (PUBLIC) -> accessible to Employee (count = 3)
        // Doc 3 (HR) -> accessible to HR (count = 3 + 1 = 4)
        // Doc 4 (ADMIN) -> accessible to Admin (count = 4 + 1 = 5)
        assertEquals(3, employeeDashboard.getTotalActiveDocuments(), "ROLE_EMPLOYEE must only see count of PUBLIC and EMPLOYEE active documents.");
        assertEquals(4, hrDashboard.getTotalActiveDocuments(), "ROLE_HR must see count of PUBLIC, EMPLOYEE, and HR active documents.");
        assertEquals(5, adminDashboard.getTotalActiveDocuments(), "ROLE_ADMIN must see count of all active documents.");

        assertTrue(employeeDashboard.getTotalActiveDocuments() < hrDashboard.getTotalActiveDocuments(),
                "HR active count must be greater than Employee active count.");
        assertTrue(hrDashboard.getTotalActiveDocuments() < adminDashboard.getTotalActiveDocuments(),
                "Admin active count must be greater than HR active count.");
    }
}

