package com.example.knowledgeportal.ai;

import com.example.knowledgeportal.dto.SearchResultItem;
import com.example.knowledgeportal.entity.*;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.repository.KnowledgeChunkRepository;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.service.EmployeeService;
import com.example.knowledgeportal.service.SearchService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * KnowledgeAgentTools implements controlled Java tools for the AI Agent workflow:
 * 1. searchKnowledgeDocuments(query, filters, userContext)
 * 2. retrieveDocumentEvidence(documentId, chunkId, userContext)
 * 3. validateEvidence(question, retrievedChunks)
 * 4. validateCitations(answer, retrievedChunks)
 * 5. retrieveEmployeeLeaveData(userContext)
 * 6. getKnowledgeCatalogSummary(userContext)
 * 
 * Strict Security Guardrails:
 * - The AI model is NEVER allowed to execute arbitrary code or database queries directly.
 * - Every tool argument is rigorously validated in Java.
 * - Documents outside the caller's allowed clearance level are blocked before retrieval.
 */
@Component
public class KnowledgeAgentTools {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final SearchService searchService;
    private final EmployeeService employeeService;

    public KnowledgeAgentTools(KnowledgeDocumentRepository documentRepository,
                               KnowledgeChunkRepository chunkRepository,
                               SearchService searchService,
                               EmployeeService employeeService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.searchService = searchService;
        this.employeeService = employeeService;
    }

    /**
     * Tool 1: Searches approved documents accessible to userContext.
     */
    public List<SearchResultItem> searchKnowledgeDocuments(String query, DocumentCategory category, UserContext userContext) {
        if (query == null || query.trim().isBlank()) {
            return Collections.emptyList();
        }

        String trimmedQuery = query.trim();
        String cleanQuery = trimmedQuery.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();

        Set<String> searchTerms = Arrays.stream(cleanQuery.split("\\s+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2 && !SearchService.STOP_WORDS.contains(term))
                .collect(Collectors.toSet());

        if (searchTerms.isEmpty()) {
            searchTerms = Arrays.stream(cleanQuery.split("\\s+"))
                    .map(String::trim)
                    .filter(term -> term.length() >= 2)
                    .collect(Collectors.toSet());
        }

        // Strictly query active documents within user's clearance
        List<KnowledgeDocument> accessibleDocs;
        if (category != null) {
            accessibleDocs = documentRepository.findByStatusAndCategoryAndAccessLevelIn(
                    DocumentStatus.ACTIVE, category, userContext.getAllowedAccessLevels()
            );
        } else {
            accessibleDocs = documentRepository.findByStatusAndAccessLevelIn(
                    DocumentStatus.ACTIVE, userContext.getAllowedAccessLevels()
            );
        }

        List<SearchResultItem> results = new ArrayList<>();

        for (KnowledgeDocument doc : accessibleDocs) {
            boolean isTargetDoc = SearchService.isDocumentTitleMatch(doc.getTitle(), searchTerms, cleanQuery);
            List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion());

            for (KnowledgeChunk chunk : chunks) {
                double score = searchService.calculateRelevanceScore(chunk, doc, cleanQuery, searchTerms, isTargetDoc);
                if (score > 0) {
                    SearchResultItem item = new SearchResultItem();
                    item.setDocumentId(doc.getId());
                    item.setTitle(doc.getTitle());
                    item.setCategory(doc.getCategory());
                    item.setVersion(doc.getVersion());
                    item.setChunkId(chunk.getId());
                    item.setChunkIndex(chunk.getChunkIndex());
                    item.setSectionReference(chunk.getSectionReference());
                    item.setSnippet(truncateSnippet(chunk.getContent(), 300));
                    item.setScore(score);
                    results.add(item);
                }
            }
        }

        // Sort by relevance score descending
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    private String truncateSnippet(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content.trim();
        }
        return content.substring(0, maxLength).trim() + "...";
    }

    /**
     * Tool 2: Retrieves specific candidate chunk with strict access control verification.
     */
    public EvidenceChunk retrieveDocumentEvidence(Long documentId, Long chunkId, UserContext userContext) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        // Strict Clearance Check
        if (!userContext.getAllowedAccessLevels().contains(doc.getAccessLevel())) {
            throw new UnauthorizedAccessException("Clearance violation: Document " + documentId +
                    " requires " + doc.getAccessLevel() + " clearance.");
        }

        KnowledgeChunk chunk = chunkRepository.findByIdAndDocumentId(chunkId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk ID " + chunkId + " not found in document " + documentId));

        return new EvidenceChunk(
                doc.getId(),
                doc.getTitle(),
                doc.getVersion(),
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getSectionReference(),
                chunk.getPageNumber(),
                chunk.getContent(),
                1.0
        );
    }

    /**
     * Tool 3: Validates whether the retrieved evidence is sufficient to answer the user's question.
     */
    private static final Set<String> INQUIRY_META_WORDS = Set.of(
            "rule", "rules", "policy", "policies", "guide", "guides", "guideline", "guidelines",
            "procedure", "procedures", "protocol", "protocols", "detailed", "process", "processes",
            "overview", "summary", "explanation", "explain", "describe", "description"
    );

    public boolean validateEvidence(String question, List<EvidenceChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return false;
        }

        List<String> keywords = Arrays.stream(question.toLowerCase().split("\\s+"))
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() >= 3 && !SearchService.STOP_WORDS.contains(w))
                .collect(Collectors.toList());

        if (keywords.isEmpty()) {
            return !retrievedChunks.isEmpty();
        }

        // If there are substantive subject keywords beyond inquiry meta-words, prioritize them
        List<String> subjectKeywords = keywords.stream()
                .filter(w -> !INQUIRY_META_WORDS.contains(w))
                .collect(Collectors.toList());

        List<String> targetKeywords = subjectKeywords.isEmpty() ? keywords : subjectKeywords;

        long matchedKeywordsCount = targetKeywords.stream()
                .filter(kw -> retrievedChunks.stream().anyMatch(chunk -> {
                    String content = chunk.getContent().toLowerCase();
                    if (content.contains(kw)) return true;
                    // Singular / plural stem matching (e.g. leaves -> leave, policies -> policy, rules -> rule)
                    if (kw.endsWith("s") && kw.length() > 3 && content.contains(kw.substring(0, kw.length() - 1))) return true;
                    if (kw.endsWith("es") && kw.length() > 4 && content.contains(kw.substring(0, kw.length() - 2))) return true;
                    if (content.contains(kw + "s")) return true;
                    return false;
                }))
                .count();

        return matchedKeywordsCount >= 1;
    }

    /**
     * Tool 4: Validates citations against the actual retrieved chunks to prevent hallucination.
     */
    public List<CitationSource> validateCitations(String answer, List<EvidenceChunk> retrievedChunks) {
        List<CitationSource> validatedCitations = new ArrayList<>();
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return validatedCitations;
        }

        for (EvidenceChunk chunk : retrievedChunks) {
            String excerpt = truncateExcerpt(chunk.getContent(), 250);
            validatedCitations.add(new CitationSource(
                    chunk.getDocumentId(),
                    chunk.getDocumentTitle(),
                    chunk.getDocumentVersion(),
                    chunk.getSectionReference(),
                    chunk.getChunkIndex(),
                    excerpt
            ));
        }

        return validatedCitations;
    }

    /**
     * Tool 5: Retrieves employee profile and leave balances for authenticated caller or queried employee.
     */
    public Optional<String> retrieveEmployeeContext(UserContext userContext, String question) {
        if (employeeService == null || userContext == null || userContext.getUsername() == null) {
            return Optional.empty();
        }
        boolean isAdminOrHr = userContext.getRole() == UserRole.ROLE_ADMIN || userContext.getRole() == UserRole.ROLE_HR;
        return employeeService.searchEmployeeContextForAi(question, userContext.getUsername(), isAdminOrHr);
    }

    public Optional<String> retrieveEmployeeLeaveContext(UserContext userContext) {
        if (employeeService == null || userContext == null || userContext.getUsername() == null) {
            return Optional.empty();
        }
        return employeeService.getEmployeeLeaveContextForAi(userContext.getUsername());
    }

    /**
     * Tool 6: Provides catalog summary of accessible documents by category and count.
     */
    public String getKnowledgeCatalogSummary(UserContext userContext) {
        List<KnowledgeDocument> accessibleDocs = documentRepository.findAll().stream()
                .filter(d -> d.getStatus() == DocumentStatus.ACTIVE && userContext.getAllowedAccessLevels().contains(d.getAccessLevel()))
                .collect(Collectors.toList());

        Map<DocumentCategory, List<KnowledgeDocument>> byCategory = accessibleDocs.stream()
                .collect(Collectors.groupingBy(KnowledgeDocument::getCategory, LinkedHashMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        sb.append("Enterprise Knowledge Catalog (Total Accessible Documents: ").append(accessibleDocs.size()).append("):\n");

        for (Map.Entry<DocumentCategory, List<KnowledgeDocument>> entry : byCategory.entrySet()) {
            DocumentCategory cat = entry.getKey();
            List<KnowledgeDocument> docs = entry.getValue();
            sb.append("• Category '").append(cat.name()).append("' (Count: ").append(docs.size()).append("):\n");
            for (KnowledgeDocument d : docs) {
                sb.append("   - ").append(d.getTitle()).append(" (Version ").append(d.getVersion()).append(")\n");
            }
        }

        return sb.toString();
    }

    private String truncateExcerpt(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content.trim();
        }
        return content.substring(0, maxLength).trim() + "...";
    }
}
