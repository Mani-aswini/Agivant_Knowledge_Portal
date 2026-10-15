package com.example.knowledgeportal.service;

import com.example.knowledgeportal.dto.FeedbackAdminItemResponse;
import com.example.knowledgeportal.dto.FeedbackManagementResponse;
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

import java.util.*;
import java.util.stream.Collectors;

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

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Question reference is required.");
        }

        if (request.getAnswerReference() == null || request.getAnswerReference().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer reference is required.");
        }

        if (request.getComment() != null && request.getComment().length() > 1000) {
            throw new IllegalArgumentException("Feedback comment cannot exceed 1000 characters.");
        }

        // Verify document exists and caller has clearance if documentId is provided
        if (request.getDocumentId() != null) {
            KnowledgeDocument document = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Referenced document not found with ID: " + request.getDocumentId()));
            permissionService.validateDocumentAccess(auth, document);
        }

        String username = permissionService.getCurrentUsername(auth);
        KnowledgeFeedback feedback = new KnowledgeFeedback(
                request.getDocumentId(),
                request.getQuestion().trim(),
                request.getAnswerReference().trim(),
                request.getRating(),
                request.getComment() != null ? request.getComment().trim() : "",
                username,
                request.getAnswerText()
        );

        KnowledgeFeedback saved = feedbackRepository.save(feedback);
        return new FeedbackResponse(saved, "Thank you! Your feedback has been recorded successfully.");
    }

    /**
     * Retrieves feedback analytics and review table for administrators and HR managers.
     */
    @Transactional(readOnly = true)
    public FeedbackManagementResponse getFeedbackManagement(Integer ratingFilter, String search, Authentication auth) {
        permissionService.validateAdminOrHr(auth);

        List<KnowledgeFeedback> allFeedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        long totalSubmissions = allFeedbacks.size();

        // Calculate 1 to 5 star distribution
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }
        double sumRatings = 0.0;
        for (KnowledgeFeedback fb : allFeedbacks) {
            distribution.put(fb.getRating(), distribution.getOrDefault(fb.getRating(), 0L) + 1L);
            sumRatings += fb.getRating();
        }
        double avgRating = (totalSubmissions > 0) ? Math.round((sumRatings / totalSubmissions) * 10.0) / 10.0 : 0.0;
        boolean hasRatings = totalSubmissions > 0;

        // Apply filters
        List<KnowledgeFeedback> filtered = allFeedbacks.stream()
                .filter(fb -> {
                    if (ratingFilter != null && fb.getRating() != ratingFilter) {
                        return false;
                    }
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean matchQ = fb.getQuestion() != null && fb.getQuestion().toLowerCase().contains(s);
                        boolean matchC = fb.getComment() != null && fb.getComment().toLowerCase().contains(s);
                        boolean matchU = fb.getUserId() != null && fb.getUserId().toLowerCase().contains(s);
                        boolean matchRef = fb.getAnswerReference() != null && fb.getAnswerReference().toLowerCase().contains(s);
                        return matchQ || matchC || matchU || matchRef;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Map<Long, KnowledgeDocument> docCache = new HashMap<>();

        List<FeedbackAdminItemResponse> itemResponses = filtered.stream().map(fb -> {
            FeedbackAdminItemResponse item = new FeedbackAdminItemResponse();
            item.setId(fb.getId());
            item.setQuestion(fb.getQuestion());
            item.setAnswerReference(fb.getAnswerReference());
            item.setAnswerText(fb.getAnswerText());
            item.setRating(fb.getRating());
            item.setComment(fb.getComment());
            item.setUserId(fb.getUserId());
            item.setCreatedAt(fb.getCreatedAt());

            if (fb.getDocumentId() != null) {
                KnowledgeDocument doc = docCache.computeIfAbsent(fb.getDocumentId(),
                        id -> documentRepository.findById(id).orElse(null));
                if (doc != null) {
                    item.setDocumentId(doc.getId());
                    item.setDocumentTitle(doc.getTitle());
                    item.setDocumentCategory(doc.getCategory());
                    item.setCustomCategory(doc.getCustomCategory());
                    item.setDocumentVersion(doc.getVersion());
                }
            }

            return item;
        }).collect(Collectors.toList());

        return new FeedbackManagementResponse(totalSubmissions, avgRating, hasRatings, distribution, itemResponses);
    }
}
