package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.ai.AiResearchAgent;
import com.example.knowledgeportal.ai.AskRequest;
import com.example.knowledgeportal.ai.AskResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for the AI Research Agent Q&A endpoint.
 */
@RestController
@RequestMapping("/api/knowledge/ask")
public class AiAgentController {

    private final AiResearchAgent aiResearchAgent;

    public AiAgentController(AiResearchAgent aiResearchAgent) {
        this.aiResearchAgent = aiResearchAgent;
    }

    @PostMapping
    public ResponseEntity<AskResponse> askQuestion(@Valid @RequestBody AskRequest request,
                                                  Authentication authentication) {
        AskResponse response = aiResearchAgent.ask(request, authentication);
        return ResponseEntity.ok(response);
    }
}
