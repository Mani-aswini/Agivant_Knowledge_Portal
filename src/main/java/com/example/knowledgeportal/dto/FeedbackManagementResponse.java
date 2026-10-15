package com.example.knowledgeportal.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO representing aggregated feedback management review and analytics.
 */
public class FeedbackManagementResponse {

    private long totalSubmissions;
    private double averageRating;
    private boolean hasRatings;
    private Map<Integer, Long> ratingDistribution;
    private List<FeedbackAdminItemResponse> items;

    public FeedbackManagementResponse() {
    }

    public FeedbackManagementResponse(long totalSubmissions, double averageRating, boolean hasRatings,
                                      Map<Integer, Long> ratingDistribution, List<FeedbackAdminItemResponse> items) {
        this.totalSubmissions = totalSubmissions;
        this.averageRating = averageRating;
        this.hasRatings = hasRatings;
        this.ratingDistribution = ratingDistribution;
        this.items = items;
    }

    public long getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(long totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public boolean isHasRatings() {
        return hasRatings;
    }

    public void setHasRatings(boolean hasRatings) {
        this.hasRatings = hasRatings;
    }

    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public List<FeedbackAdminItemResponse> getItems() {
        return items;
    }

    public void setItems(List<FeedbackAdminItemResponse> items) {
        this.items = items;
    }
}
