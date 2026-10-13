package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.dto.FeedbackRequest;
import com.example.knowledgeportal.dto.FeedbackResponse;
import com.example.knowledgeportal.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for submitting and recording user feedback on generated answers.
 */
@RestController
@RequestMapping("/api/knowledge/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackRequest request,
                                                           Authentication authentication) {
        FeedbackResponse response = feedbackService.submitFeedback(request, authentication);
        return ResponseEntity.ok(response);
    }
}
