package com.example.knowledgeportal.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {

    private long totalActiveDocuments;
    private long totalArchivedDocuments;
    private long totalAwaitingReindex;
    private long totalFeedbackCount;
    private double overallAverageRating;

    private List<Map<String, Object>> documentsByCategoryAndStatus;
    private List<Map<String, Object>> documentsAwaitingReindex;
    private List<Map<String, Object>> frequentlySearchedCategories;
    private List<Map<String, Object>> feedbackRatingsByDocument;

    public AnalyticsResponse() {
    }

    // Getters and Setters
    public long getTotalActiveDocuments() {
        return totalActiveDocuments;
    }

    public void setTotalActiveDocuments(long totalActiveDocuments) {
        this.totalActiveDocuments = totalActiveDocuments;
    }

    public long getTotalArchivedDocuments() {
        return totalArchivedDocuments;
    }

    public void setTotalArchivedDocuments(long totalArchivedDocuments) {
        this.totalArchivedDocuments = totalArchivedDocuments;
    }

    public long getTotalAwaitingReindex() {
        return totalAwaitingReindex;
    }

    public void setTotalAwaitingReindex(long totalAwaitingReindex) {
        this.totalAwaitingReindex = totalAwaitingReindex;
    }

    public long getTotalFeedbackCount() {
        return totalFeedbackCount;
    }

    public void setTotalFeedbackCount(long totalFeedbackCount) {
        this.totalFeedbackCount = totalFeedbackCount;
    }

    public double getOverallAverageRating() {
        return overallAverageRating;
    }

    public void setOverallAverageRating(double overallAverageRating) {
        this.overallAverageRating = overallAverageRating;
    }

    public List<Map<String, Object>> getDocumentsByCategoryAndStatus() {
        return documentsByCategoryAndStatus;
    }

    public void setDocumentsByCategoryAndStatus(List<Map<String, Object>> documentsByCategoryAndStatus) {
        this.documentsByCategoryAndStatus = documentsByCategoryAndStatus;
    }

    public List<Map<String, Object>> getDocumentsAwaitingReindex() {
        return documentsAwaitingReindex;
    }

    public void setDocumentsAwaitingReindex(List<Map<String, Object>> documentsAwaitingReindex) {
        this.documentsAwaitingReindex = documentsAwaitingReindex;
    }

    public List<Map<String, Object>> getFrequentlySearchedCategories() {
        return frequentlySearchedCategories;
    }

    public void setFrequentlySearchedCategories(List<Map<String, Object>> frequentlySearchedCategories) {
        this.frequentlySearchedCategories = frequentlySearchedCategories;
    }

    public List<Map<String, Object>> getFeedbackRatingsByDocument() {
        return feedbackRatingsByDocument;
    }

    public void setFeedbackRatingsByDocument(List<Map<String, Object>> feedbackRatingsByDocument) {
        this.feedbackRatingsByDocument = feedbackRatingsByDocument;
    }
}
