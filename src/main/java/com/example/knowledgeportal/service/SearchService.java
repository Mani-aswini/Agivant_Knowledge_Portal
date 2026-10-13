package com.example.knowledgeportal.service;

import com.example.knowledgeportal.dto.SearchRequest;
import com.example.knowledgeportal.dto.SearchResponse;
import com.example.knowledgeportal.dto.SearchResultItem;
import com.example.knowledgeportal.entity.*;
import com.example.knowledgeportal.repository.KnowledgeChunkRepository;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.repository.SearchHistoryRepository;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchService handles keyword-based retrieval and relevance scoring.
 * 
 * Beginner Concept:
 * Unlike black-box vector embeddings, keyword retrieval uses deterministic lexical
 * term frequencies. We compute an understandable relevance score:
 * - Exact phrase match: +10 points
 * - Individual term frequency in chunk: +2 points per occurrence
 * - Document title match: +5 points
 * - Section reference match: +3 points
 * 
 * Security Rule:
 * We strictly query documents accessible to the authenticated user's clearance level.
 * Restricted documents are excluded BEFORE any scoring or retrieval occurs.
 */
@Service
public class SearchService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final PermissionService permissionService;

    public SearchService(KnowledgeDocumentRepository documentRepository,
                         KnowledgeChunkRepository chunkRepository,
                         SearchHistoryRepository searchHistoryRepository,
                         PermissionService permissionService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public SearchResponse search(SearchRequest request, Authentication auth) {
        String query = request.getQuery();
        if (query == null || query.trim().isBlank()) {
            return new SearchResponse("", 0, Collections.emptyList());
        }

        String trimmedQuery = query.trim();
        String lowerQuery = trimmedQuery.toLowerCase();
        Set<String> searchTerms = Arrays.stream(lowerQuery.split("\\s+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .collect(Collectors.toSet());

        // 1. Get allowable access levels for current user
        Set<AccessLevel> allowedAccessLevels = permissionService.getAllowedAccessLevels(auth);
        DocumentStatus status = (request.getStatus() != null) ? request.getStatus() : DocumentStatus.ACTIVE;

        // 2. Retrieve only accessible documents
        List<KnowledgeDocument> accessibleDocuments;
        if (request.getCategory() != null) {
            accessibleDocuments = documentRepository.findByStatusAndCategoryAndAccessLevelIn(
                    status, request.getCategory(), allowedAccessLevels
            );
        } else {
            accessibleDocuments = documentRepository.findByStatusAndAccessLevelIn(
                    status, allowedAccessLevels
            );
        }

        if (accessibleDocuments.isEmpty()) {
            recordSearchHistory(trimmedQuery, request.getCategory(), 0, auth);
            return new SearchResponse(trimmedQuery, 0, Collections.emptyList());
        }

        Map<Long, KnowledgeDocument> docMap = accessibleDocuments.stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, doc -> doc));

        // 3. Retrieve chunks for the CURRENT active version of each accessible document
        List<SearchResultItem> scoredResults = new ArrayList<>();

        for (KnowledgeDocument doc : accessibleDocuments) {
            List<KnowledgeChunk> currentChunks = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion());

            for (KnowledgeChunk chunk : currentChunks) {
                double score = calculateRelevanceScore(chunk, doc, lowerQuery, searchTerms);
                if (score > 0) {
                    String snippet = createSnippet(chunk.getContent(), searchTerms);
                    scoredResults.add(new SearchResultItem(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getCategory(),
                            doc.getVersion(),
                            chunk.getId(),
                            chunk.getChunkIndex(),
                            chunk.getSectionReference(),
                            chunk.getPageNumber(),
                            snippet,
                            score
                    ));
                }
            }
        }

        // 4. Rank results descending by score
        scoredResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 5. Persist search query history for analytics
        recordSearchHistory(trimmedQuery, request.getCategory(), scoredResults.size(), auth);

        return new SearchResponse(trimmedQuery, scoredResults.size(), scoredResults);
    }

    /**
     * Scores a chunk against the search query terms and full phrase.
     */
    public double calculateRelevanceScore(KnowledgeChunk chunk, KnowledgeDocument doc,
                                         String lowerQuery, Set<String> terms) {
        double score = 0.0;
        String contentLower = (chunk.getSearchableText() != null) ? chunk.getSearchableText() : chunk.getContent().toLowerCase();
        String titleLower = doc.getTitle().toLowerCase();
        String sectionLower = (chunk.getSectionReference() != null) ? chunk.getSectionReference().toLowerCase() : "";

        // Exact full phrase bonus
        if (contentLower.contains(lowerQuery)) {
            score += 10.0;
        }

        // Term frequency match in chunk body
        for (String term : terms) {
            int occurrences = countOccurrences(contentLower, term);
            if (occurrences > 0) {
                score += (occurrences * 2.0);
            }
            if (titleLower.contains(term)) {
                score += 5.0;
            }
            if (sectionLower.contains(term)) {
                score += 3.0;
            }
        }

        return score;
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }

    private String createSnippet(String content, Set<String> terms) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String lower = content.toLowerCase();
        int firstIndex = -1;

        for (String term : terms) {
            int idx = lower.indexOf(term);
            if (idx != -1 && (firstIndex == -1 || idx < firstIndex)) {
                firstIndex = idx;
            }
        }

        if (firstIndex == -1) {
            return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }

        int start = Math.max(0, firstIndex - 60);
        int end = Math.min(content.length(), firstIndex + 140);
        String snippet = content.substring(start, end).trim();

        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

    private void recordSearchHistory(String query, DocumentCategory category, int count, Authentication auth) {
        try {
            String username = permissionService.getCurrentUsername(auth);
            String categoryStr = (category != null) ? category.name() : null;
            searchHistoryRepository.save(new SearchHistory(query, categoryStr, count, username));
        } catch (Exception ignored) {
            // Non-critical background logging failure should not break search
        }
    }
}
