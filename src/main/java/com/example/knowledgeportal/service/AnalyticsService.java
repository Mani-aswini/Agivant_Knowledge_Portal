package com.example.knowledgeportal.service;

import com.example.knowledgeportal.dto.AnalyticsResponse;
import com.example.knowledgeportal.dto.DocumentResponse;
import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.entity.KnowledgeDocument;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.repository.KnowledgeChunkRepository;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.repository.KnowledgeFeedbackRepository;
import com.example.knowledgeportal.repository.SearchHistoryRepository;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService aggregates system data for dashboard and reporting screens.
 */
@Service
public class AnalyticsService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeFeedbackRepository feedbackRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final PermissionService permissionService;

    public AnalyticsService(KnowledgeDocumentRepository documentRepository,
                            KnowledgeChunkRepository chunkRepository,
                            KnowledgeFeedbackRepository feedbackRepository,
                            SearchHistoryRepository searchHistoryRepository,
                            PermissionService permissionService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.feedbackRepository = feedbackRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalyticsDashboard() {
        AnalyticsResponse response = new AnalyticsResponse();

        response.setTotalActiveDocuments(documentRepository.countByStatus(DocumentStatus.ACTIVE));
        response.setTotalArchivedDocuments(documentRepository.countByStatus(DocumentStatus.ARCHIVED));
        response.setTotalAwaitingReindex(documentRepository.countByNeedsReindexTrueAndStatus(DocumentStatus.ACTIVE));
        response.setTotalFeedbackCount(feedbackRepository.count());

        Double avgRating = feedbackRepository.getOverallAverageRating();
        response.setOverallAverageRating(avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : 0.0);

        // 1. Documents grouped by category and status
        List<Object[]> catStatusList = documentRepository.countGroupedByCategoryAndStatus();
        List<Map<String, Object>> catStatusMapList = new ArrayList<>();
        for (Object[] row : catStatusList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("category", row[0] != null ? row[0].toString() : "UNKNOWN");
            map.put("status", row[1] != null ? row[1].toString() : "UNKNOWN");
            map.put("count", row[2] != null ? ((Number) row[2]).longValue() : 0L);
            catStatusMapList.add(map);
        }
        response.setDocumentsByCategoryAndStatus(catStatusMapList);

        // 2. Documents awaiting re-indexing
        List<KnowledgeDocument> reindexDocs = documentRepository.findByNeedsReindexTrueAndStatus(DocumentStatus.ACTIVE);
        List<Map<String, Object>> reindexList = new ArrayList<>();
        for (KnowledgeDocument doc : reindexDocs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", doc.getId());
            map.put("title", doc.getTitle());
            map.put("category", doc.getCategory().name());
            map.put("version", doc.getVersion());
            map.put("updatedAt", doc.getUpdatedAt().toString());
            reindexList.add(map);
        }
        response.setDocumentsAwaitingReindex(reindexList);

        // 3. Most frequently searched categories
        List<Object[]> topCategories = searchHistoryRepository.findTopSearchedCategories();
        List<Map<String, Object>> topCategoriesList = new ArrayList<>();
        for (Object[] row : topCategories) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("category", row[0] != null ? row[0].toString() : "ALL");
            map.put("searchCount", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            map.put("avgResults", row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0.0);
            topCategoriesList.add(map);
        }
        response.setFrequentlySearchedCategories(topCategoriesList);

        // 4. Feedback ratings grouped by document
        List<Object[]> feedbackStats = feedbackRepository.findFeedbackStatsGroupedByDocument();
        List<Map<String, Object>> feedbackStatsList = new ArrayList<>();
        for (Object[] row : feedbackStats) {
            Long docId = ((Number) row[0]).longValue();
            Optional<KnowledgeDocument> docOpt = documentRepository.findById(docId);
            String title = docOpt.map(KnowledgeDocument::getTitle).orElse("Document #" + docId);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("documentId", docId);
            map.put("title", title);
            map.put("feedbackCount", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            map.put("averageRating", row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0.0);
            map.put("fiveStarCount", row[3] != null ? ((Number) row[3]).longValue() : 0L);
            map.put("lowRatingCount", row[4] != null ? ((Number) row[4]).longValue() : 0L);
            feedbackStatsList.add(map);
        }
        response.setFeedbackRatingsByDocument(feedbackStatsList);

        return response;
    }

    /**
     * SQL Task 5: Return documents accessible to a given role.
     * Restricted to administrators.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsAccessibleToRole(UserRole targetRole, Authentication auth) {
        if (!permissionService.isUserAdmin(auth)) {
            throw new UnauthorizedAccessException("Role accessibility simulation is restricted to administrators.");
        }

        Set<AccessLevel> targetLevels;
        switch (targetRole) {
            case ROLE_ADMIN:
                targetLevels = EnumSet.allOf(AccessLevel.class);
                break;
            case ROLE_HR:
                targetLevels = EnumSet.of(AccessLevel.PUBLIC, AccessLevel.EMPLOYEE, AccessLevel.HR);
                break;
            case ROLE_EMPLOYEE:
            default:
                targetLevels = EnumSet.of(AccessLevel.PUBLIC, AccessLevel.EMPLOYEE);
                break;
        }

        List<KnowledgeDocument> accessibleDocs = documentRepository.findAccessibleDocuments(DocumentStatus.ACTIVE, targetLevels);
        return accessibleDocs.stream()
                .map(doc -> {
                    int count = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion()).size();
                    return new DocumentResponse(doc, count);
                })
                .collect(Collectors.toList());
    }
}
