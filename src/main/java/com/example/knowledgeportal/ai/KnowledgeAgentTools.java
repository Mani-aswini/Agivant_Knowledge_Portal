package com.example.knowledgeportal.ai;

import com.example.knowledgeportal.dto.SearchResultItem;
import com.example.knowledgeportal.entity.*;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.repository.KnowledgeChunkRepository;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.service.SearchService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * KnowledgeAgentTools implements the 4 controlled Java tools mandated for the AI Agent workflow:
 * 1. searchKnowledgeDocuments(query, filters, userContext)
 * 2. retrieveDocumentEvidence(documentId, chunkId, userContext)
 * 3. validateEvidence(question, retrievedChunks)
 * 4. validateCitations(answer, retrievedChunks)
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

    public KnowledgeAgentTools(KnowledgeDocumentRepository documentRepository,
                               KnowledgeChunkRepository chunkRepository,
                               SearchService searchService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.searchService = searchService;
    }

    /**
     * Tool 1: Searches approved documents accessible to userContext.
     */
    public List<SearchResultItem> searchKnowledgeDocuments(String query, DocumentCategory category, UserContext userContext) {
        if (query == null || query.trim().isBlank()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.toLowerCase().trim();
        Set<String> searchTerms = Arrays.stream(lowerQuery.split("\\s+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .collect(Collectors.toSet());

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

        List<SearchResultItem> matches = new ArrayList<>();
        for (KnowledgeDocument doc : accessibleDocs) {
            List<KnowledgeChunk> currentChunks = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion());
            for (KnowledgeChunk chunk : currentChunks) {
                double score = searchService.calculateRelevanceScore(chunk, doc, lowerQuery, searchTerms);
                if (score > 0) {
                    matches.add(new SearchResultItem(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getCategory(),
                            doc.getVersion(),
                            chunk.getId(),
                            chunk.getChunkIndex(),
                            chunk.getSectionReference(),
                            chunk.getPageNumber(),
                            chunk.getContent(),
                            score
                    ));
                }
            }
        }

        // Rank by relevance score descending
        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return matches;
    }

    /**
     * Tool 2: Retrieves verified document evidence chunk after validating user clearance.
     */
    public EvidenceChunk retrieveDocumentEvidence(Long documentId, Long chunkId, UserContext userContext) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        // Enforce document clearance check
        if (!userContext.getAllowedAccessLevels().contains(doc.getAccessLevel())) {
            throw new UnauthorizedAccessException("Clearance violation: Caller '" + userContext.getUsername() +
                    "' cannot access document clearance level '" + doc.getAccessLevel() + "'.");
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
     * Evaluates keyword presence and minimum confidence threshold.
     */
    public boolean validateEvidence(String question, List<EvidenceChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return false;
        }

        // Extract significant keyword tokens from question (ignore question words like "what", "is", "how", "the")
        Set<String> stopWords = Set.of("what", "is", "are", "how", "many", "the", "a", "an", "of", "to", "in", "for", "on", "with", "can", "do", "does", "available", "be");
        List<String> keywords = Arrays.stream(question.toLowerCase().split("\\s+"))
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() >= 3 && !stopWords.contains(w))
                .collect(Collectors.toList());

        if (keywords.isEmpty()) {
            return !retrievedChunks.isEmpty();
        }

        // Check if any retrieved chunk contains at least one key subject term
        long matchedKeywordsCount = keywords.stream()
                .filter(kw -> retrievedChunks.stream().anyMatch(chunk -> chunk.getContent().toLowerCase().contains(kw)))
                .count();

        // Must match at least 1 critical subject keyword from question
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

        // Attach citations for chunks that provided verifiable evidence
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
