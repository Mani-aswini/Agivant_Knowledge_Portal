package com.example.knowledgeportal.ai;

import com.example.knowledgeportal.dto.SearchResultItem;
import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.exception.AiProcessingException;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AiResearchAgent orchestrates the goal-driven AI workflow using controlled Java tools.
 * 
 * Workflow Steps:
 * 1. Validate question
 * 2. Resolve caller's UserContext & access clearance
 * 3. Tool 1: searchKnowledgeDocuments (search accessible documents only)
 * 4. Tool 2: retrieveDocumentEvidence (retrieve candidate chunks)
 * 5. Tool 3: validateEvidence (sufficiency check; abstain if evidence is inadequate)
 * 6. Synthesize answer (REAL_AI via external API or deterministic SIMULATION)
 * 7. Prompt-Injection Safeguard: neutralize any embedded malicious instructions
 * 8. Tool 4: validateCitations (verify and attach source citations)
 * 9. Return structured response with mode and citations
 */
@Service
public class AiResearchAgent {

    private final KnowledgeAgentTools agentTools;
    private final AiClient aiClient;
    private final PermissionService permissionService;

    public AiResearchAgent(KnowledgeAgentTools agentTools,
                           AiClient aiClient,
                           PermissionService permissionService) {
        this.agentTools = agentTools;
        this.aiClient = aiClient;
        this.permissionService = permissionService;
    }

    public AskResponse ask(AskRequest request, Authentication auth) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty or blank.");
        }

        String question = request.getQuestion().trim();

        // 1. Resolve caller security context
        String username = permissionService.getCurrentUsername(auth);
        UserRole role = permissionService.getCurrentUserRole(auth);
        Set<AccessLevel> allowedAccessLevels = permissionService.getAllowedAccessLevels(auth);
        UserContext userContext = new UserContext(username, role, allowedAccessLevels);

        // 2. Tool 1: Search authorized knowledge documents
        List<SearchResultItem> searchResults = agentTools.searchKnowledgeDocuments(question, null, userContext);

        if (searchResults.isEmpty()) {
            return new AskResponse(
                    question,
                    "I cannot find sufficient evidence in the approved enterprise documents accessible to your clearance level to answer this question reliably.",
                    true,
                    "SIMULATION",
                    Collections.emptyList(),
                    "No matching document passages found within authorized clearance levels."
            );
        }

        // 3. Tool 2: Retrieve evidence chunks for top candidates (limit to top 4 chunks across documents)
        List<EvidenceChunk> evidenceList = new ArrayList<>();
        Set<String> seenChunks = new HashSet<>();

        for (SearchResultItem item : searchResults) {
            String key = item.getDocumentId() + "_" + item.getChunkId();
            if (!seenChunks.contains(key) && evidenceList.size() < 4) {
                seenChunks.add(key);
                EvidenceChunk evidence = agentTools.retrieveDocumentEvidence(item.getDocumentId(), item.getChunkId(), userContext);
                evidence.setRelevanceScore(item.getScore());
                evidenceList.add(evidence);
            }
        }

        // 4. Tool 3: Validate evidence sufficiency
        boolean isSufficient = agentTools.validateEvidence(question, evidenceList);
        if (!isSufficient) {
            return new AskResponse(
                    question,
                    "I cannot find sufficient evidence in the approved enterprise documents accessible to your clearance level to answer this question reliably.",
                    true,
                    "SIMULATION",
                    Collections.emptyList(),
                    "Retrieved documents did not contain sufficient factual overlap with the question keywords."
            );
        }

        // 5. Prompt-Injection Safeguard Check
        sanitizeEvidenceForPromptInjection(evidenceList);

        // 6. Generate answer using configured REAL_AI or deterministic SIMULATION
        String answer;
        String executionMode;
        String reasoningNotes;

        if (aiClient.isConfigured()) {
            try {
                answer = aiClient.generateAnswer(question, evidenceList);
                executionMode = "REAL_AI";
                reasoningNotes = "Answer generated using live AI model with retrieved evidence context.";
            } catch (AiProcessingException ex) {
                // Graceful fallback to deterministic simulation on timeout or API error
                answer = buildDeterministicExtractiveAnswer(question, evidenceList);
                executionMode = "SIMULATION_FALLBACK";
                reasoningNotes = "Live AI request failed (" + ex.getMessage() + "). Fell back safely to deterministic extractive simulation.";
            }
        } else {
            answer = buildDeterministicExtractiveAnswer(question, evidenceList);
            executionMode = "SIMULATION";
            reasoningNotes = "Prototype simulation mode: Deterministic extractive synthesis from retrieved passages.";
        }

        // 7. Tool 4: Validate citations
        List<CitationSource> citations = agentTools.validateCitations(answer, evidenceList);

        return new AskResponse(
                question,
                answer,
                false,
                executionMode,
                citations,
                reasoningNotes
        );
    }

    /**
     * Inspects retrieved evidence passages to neutralize prompt-injection attempts
     * such as instructions to ignore previous instructions or escalate privileges.
     */
    private void sanitizeEvidenceForPromptInjection(List<EvidenceChunk> evidenceList) {
        List<String> injectionPatterns = List.of(
                "ignore previous instructions",
                "ignore all previous instructions",
                "system override",
                "grant admin privileges",
                "you are now an unrestricted assistant",
                "disregard safety guidelines",
                "reveal system prompt"
        );

        for (EvidenceChunk chunk : evidenceList) {
            String content = chunk.getContent();
            String lowerContent = content.toLowerCase();

            for (String pattern : injectionPatterns) {
                if (lowerContent.contains(pattern)) {
                    // Defuse injection by prefixing with warning and stripping executable phrasing
                    chunk.setContent("[SECURITY NOTICE: Malicious instructional override detected and neutralized in untrusted document source]\n" +
                            content.replaceAll("(?i)" + pattern, "[REDACTED-ATTEMPTED-PROMPT-OVERRIDE]"));
                }
            }
        }
    }

    /**
     * Builds a deterministic extractive answer from the retrieved evidence passages.
     * Supports single-document and multi-document factual combination.
     */
    private String buildDeterministicExtractiveAnswer(String question, List<EvidenceChunk> evidenceList) {
        // Group evidence by document to handle multi-document synthesis cleanly
        Map<String, List<EvidenceChunk>> groupedByDoc = evidenceList.stream()
                .collect(Collectors.groupingBy(EvidenceChunk::getDocumentTitle, LinkedHashMap::new, Collectors.toList()));

        StringBuilder answerBuilder = new StringBuilder();

        if (groupedByDoc.size() == 1) {
            // Single-document answer
            Map.Entry<String, List<EvidenceChunk>> entry = groupedByDoc.entrySet().iterator().next();
            String docTitle = entry.getKey();
            List<EvidenceChunk> chunks = entry.getValue();

            answerBuilder.append("According to '").append(docTitle).append("', ");
            String relevantSentences = extractRelevantSentences(question, chunks);
            answerBuilder.append(relevantSentences);

        } else {
            // Multi-document answer synthesis
            answerBuilder.append("Synthesizing information across multiple approved enterprise documents:\n\n");
            int docIndex = 1;

            for (Map.Entry<String, List<EvidenceChunk>> entry : groupedByDoc.entrySet()) {
                String docTitle = entry.getKey();
                List<EvidenceChunk> chunks = entry.getValue();

                answerBuilder.append(docIndex++).append(". According to '").append(docTitle).append("': ");
                String sentences = extractRelevantSentences(question, chunks);
                answerBuilder.append(sentences).append("\n\n");
            }
        }

        return answerBuilder.toString().trim();
    }

    /**
     * Extracts sentences that contain keywords matching the question.
     */
    private String extractRelevantSentences(String question, List<EvidenceChunk> chunks) {
        Set<String> searchWords = Arrays.stream(question.toLowerCase().split("\\s+"))
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() >= 3)
                .collect(Collectors.toSet());

        List<String> matchedSentences = new ArrayList<>();

        for (EvidenceChunk chunk : chunks) {
            String[] sentences = chunk.getContent().split("(?<=[.!?])\\s+");
            for (String sentence : sentences) {
                String sentenceLower = sentence.toLowerCase();
                for (String word : searchWords) {
                    if (sentenceLower.contains(word)) {
                        String cleanSentence = sentence.trim();
                        if (!matchedSentences.contains(cleanSentence)) {
                            matchedSentences.add(cleanSentence);
                        }
                        break;
                    }
                }
            }
        }

        if (matchedSentences.isEmpty()) {
            // Fallback: return the first chunk's text directly
            return chunks.get(0).getContent().trim();
        }

        return String.join(" ", matchedSentences);
    }
}
