package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.ai.AiResearchAgent;
import com.example.knowledgeportal.ai.AskRequest;
import com.example.knowledgeportal.ai.AskResponse;
import com.example.knowledgeportal.entity.ChatMessage;
import com.example.knowledgeportal.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for the AI Research Agent Q&A and Chat Conversation History.
 */
@RestController
@RequestMapping("/api/knowledge/ask")
public class AiAgentController {

    private final AiResearchAgent aiResearchAgent;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public AiAgentController(AiResearchAgent aiResearchAgent,
                              ChatMessageRepository chatMessageRepository,
                              ObjectMapper objectMapper) {
        this.aiResearchAgent = aiResearchAgent;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<AskResponse> askQuestion(@Valid @RequestBody AskRequest request,
                                                  @RequestParam(value = "username", required = false) String usernameParam,
                                                  Authentication authentication) {
        String username = resolveUsername(usernameParam, authentication);

        // Resolve or generate conversation ID
        String convId = request.getConversationId();
        if (convId == null || convId.trim().isEmpty()) {
            convId = "conv_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        } else {
            convId = convId.trim();
        }

        // Execute AI research and synthesis
        AskResponse response = aiResearchAgent.ask(request, authentication);
        response.setConversationId(convId);

        // Persist conversation history
        try {
            // Save User message
            ChatMessage userMsg = new ChatMessage(username, convId, "user", request.getQuestion(), null, null);
            chatMessageRepository.save(userMsg);

            // Save Assistant response
            String sourcesJson = null;
            if (response.getSources() != null && !response.getSources().isEmpty()) {
                sourcesJson = objectMapper.writeValueAsString(response.getSources());
            }
            ChatMessage assistantMsg = new ChatMessage(username, convId, "assistant", response.getAnswer(),
                    sourcesJson, response.getAnswerReference());
            chatMessageRepository.save(assistantMsg);
        } catch (Exception e) {
            // Log but don't fail user request
            System.err.println("Failed to persist chat message: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@RequestParam(value = "username", required = false) String usernameParam,
                                                            @RequestParam(value = "conversationId", required = false) String conversationId,
                                                            Authentication authentication) {
        String username = resolveUsername(usernameParam, authentication);
        List<ChatMessage> history;
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            history = chatMessageRepository.findByUsernameAndConversationIdOrderByCreatedAtAsc(username, conversationId.trim());
        } else {
            history = chatMessageRepository.findByUsernameOrderByCreatedAtAsc(username);
        }
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history")
    @Transactional
    public ResponseEntity<Void> clearChatHistory(@RequestParam(value = "username", required = false) String usernameParam,
                                                @RequestParam(value = "conversationId", required = false) String conversationId,
                                                Authentication authentication) {
        String username = resolveUsername(usernameParam, authentication);
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            chatMessageRepository.deleteByUsernameAndConversationId(username, conversationId.trim());
        } else {
            chatMessageRepository.deleteByUsername(username);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the active username with strict priority given to the authenticated Spring Security principal.
     */
    private String resolveUsername(String usernameParam, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        if (usernameParam != null && !usernameParam.trim().isEmpty()) {
            return usernameParam.trim();
        }
        return "anonymous";
    }
}
