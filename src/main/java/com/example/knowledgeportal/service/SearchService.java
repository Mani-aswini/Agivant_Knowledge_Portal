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

    public static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "to", "from", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any",
            "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own",
            "same", "so", "than", "too", "very", "can", "will", "just", "should", "now", "of", "for", "with",
            "about", "against", "between", "into", "through", "during", "before", "after", "above", "below",
            "give", "me", "tell", "show", "find", "get", "please", "details", "detail", "information", "info",
            "what", "which", "who", "whom", "this", "that", "these", "those", "am", "i", "we", "you", "they",
            "them", "their", "our", "us", "it", "its", "yes", "say", "mention", "item", "items"
    );

    @Transactional
    public SearchResponse search(SearchRequest request, Authentication auth) {
        String query = request.getQuery();
        if (query == null || query.trim().isBlank()) {
            return new SearchResponse("", 0, Collections.emptyList());
        }

        String trimmedQuery = query.trim();
        String cleanQuery = trimmedQuery.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();

        // Extract content keywords by eliminating stop words
        Set<String> searchTerms = Arrays.stream(cleanQuery.split("\\s+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2 && !STOP_WORDS.contains(term))
                .collect(Collectors.toSet());

        // Fallback if all words were stop words
        if (searchTerms.isEmpty()) {
            searchTerms = Arrays.stream(cleanQuery.split("\\s+"))
                    .map(String::trim)
                    .filter(term -> term.length() >= 2)
                    .collect(Collectors.toSet());
        }

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

        // 3. Retrieve chunks for the CURRENT active version of each accessible document
        List<SearchResultItem> scoredResults = new ArrayList<>();

        for (KnowledgeDocument doc : accessibleDocuments) {
            boolean isTargetDoc = isDocumentTitleMatch(doc.getTitle(), searchTerms, cleanQuery);
            List<KnowledgeChunk> currentChunks = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion());

            for (KnowledgeChunk chunk : currentChunks) {
                double score = calculateRelevanceScore(chunk, doc, cleanQuery, searchTerms, isTargetDoc);
                if (score > 0) {
                    String snippet = createSnippet(chunk.getContent(), searchTerms);
                    SearchResultItem item = new SearchResultItem(
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
                    );
                    item.setContentType(doc.getContentType());
                    item.setOriginalFileName(doc.getOriginalFileName());
                    item.setDescription(doc.getDescription());
                    item.setCustomCategory(doc.getCustomCategory());
                    scoredResults.add(item);
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
     * Checks if a document's title matches or is closely referenced (with typo tolerance e.g. bold ~ bolt).
     */
    public static boolean isDocumentTitleMatch(String docTitle, Set<String> searchTerms, String cleanQuery) {
        if (docTitle == null) return false;
        String cleanTitle = docTitle.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        if (cleanTitle.isEmpty()) return false;

        if (cleanQuery.contains(cleanTitle)) return true;

        String[] titleTokens = cleanTitle.split("\\s+");
        for (String tTok : titleTokens) {
            if (tTok.length() < 3 || STOP_WORDS.contains(tTok)) continue;
            for (String term : searchTerms) {
                if (term.equals(tTok)) return true;
                if (term.length() >= 4 && tTok.length() >= 4 && isLevenshteinDistanceAtMostOne(term, tTok)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLevenshteinDistanceAtMostOne(String s1, String s2) {
        if (Math.abs(s1.length() - s2.length()) > 1) return false;
        int i = 0, j = 0, diff = 0;
        while (i < s1.length() && j < s2.length()) {
            if (s1.charAt(i) != s2.charAt(j)) {
                diff++;
                if (diff > 1) return false;
                if (s1.length() > s2.length()) i++;
                else if (s2.length() > s1.length()) j++;
                else { i++; j++; }
            } else {
                i++; j++;
            }
        }
        return true;
    }

    /**
     * Scores a chunk against search query terms and full phrase with semantic boosts.
     */
    public double calculateRelevanceScore(KnowledgeChunk chunk, KnowledgeDocument doc,
                                         String cleanQuery, Set<String> terms, boolean isTargetDoc) {
        double score = 0.0;
        String contentLower = (chunk.getSearchableText() != null ? chunk.getSearchableText() : chunk.getContent()).toLowerCase();
        String titleLower = doc.getTitle().toLowerCase();
        String sectionLower = (chunk.getSectionReference() != null) ? chunk.getSectionReference().toLowerCase() : "";

        // Boost target document chunks
        if (isTargetDoc) {
            score += 50.0;
        }

        // Multi-word phrase matches (e.g. "table of content", "table of contents", "workflow guide")
        if (cleanQuery.contains("table of content") || cleanQuery.contains("table of contents")) {
            if (contentLower.contains("table of content") || contentLower.contains("table of contents")) {
                score += 50.0;
            }
            if (sectionLower.contains("table of content") || sectionLower.contains("contents")) {
                score += 35.0;
            }
        }

        // Full clean query match in content
        if (cleanQuery.length() > 5 && contentLower.contains(cleanQuery)) {
            score += 25.0;
        }

        int matchedTermsCount = 0;
        for (String term : terms) {
            boolean termFound = false;

            // Content body match with stem-awareness
            if (contentLower.contains(term)) {
                int count = Math.min(5, countOccurrences(contentLower, term));
                score += (count * 4.0);
                termFound = true;
            } else if (term.endsWith("s") && contentLower.contains(term.substring(0, term.length() - 1))) {
                score += 4.0;
                termFound = true;
            } else if (!term.endsWith("s") && contentLower.contains(term + "s")) {
                score += 4.0;
                termFound = true;
            }

            if (titleLower.contains(term)) {
                score += 12.0;
                termFound = true;
            }
            if (sectionLower.contains(term)) {
                score += 8.0;
                termFound = true;
            }

            if (termFound) {
                matchedTermsCount++;
            }
        }

        // Strict grounding rule: If not the target document, require at least one genuine content term match
        if (!isTargetDoc && matchedTermsCount == 0) {
            return 0.0;
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
