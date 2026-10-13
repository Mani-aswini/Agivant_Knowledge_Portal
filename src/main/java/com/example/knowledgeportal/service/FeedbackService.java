package com.example.knowledgeportal.service;

import com.example.knowledgeportal.dto.FeedbackRequest;
import com.example.knowledgeportal.dto.FeedbackResponse;
import com.example.knowledgeportal.entity.KnowledgeDocument;
import com.example.knowledgeportal.entity.KnowledgeFeedback;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.repository.KnowledgeFeedbackRepository;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FeedbackService persists user ratings on answers and enforces authorization.
 */
@Service
public class FeedbackService {

    private final KnowledgeFeedbackRepository feedbackRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final PermissionService permissionService;

    public FeedbackService(KnowledgeFeedbackRepository feedbackRepository,
                           KnowledgeDocumentRepository documentRepository,
                           PermissionService permissionService) {
        this.feedbackRepository = feedbackRepository;
        this.documentRepository = documentRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request, Authentication auth) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Feedback rating must be an integer between 1 and 5 stars.");
        }

        // Verify document exists
        KnowledgeDocument document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Referenced document not found with ID: " + request.getDocumentId()));

        // Security check: Verify user is authorized to access the referenced document
        permissionService.validateDocumentAccess(auth, document);

        String username = permissionService.getCurrentUsername(auth);
        KnowledgeFeedback feedback = new KnowledgeFeedback(
                request.getDocumentId(),
                request.getQuestion(),
                request.getAnswerReference(),
                request.getRating(),
                request.getComment(),
                username
        );

        KnowledgeFeedback saved = feedbackRepository.save(feedback);
        return new FeedbackResponse(saved, "Thank you! Your feedback has been recorded successfully.");
    }
}
