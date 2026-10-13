package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * SearchHistory Entity
 * 
 * Logs employee search queries and filters to support analytics and search relevance tracking.
 * Strictly avoids storing sensitive contents.
 */
@Entity
@Table(name = "search_history", indexes = {
    @Index(name = "idx_search_created", columnList = "created_at"),
    @Index(name = "idx_search_category", columnList = "category_filter")
})
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(name = "category_filter", length = 50)
    private String categoryFilter;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SearchHistory() {
    }

    public SearchHistory(String query, String categoryFilter, int resultCount, String userId) {
        this.query = query;
        this.categoryFilter = categoryFilter;
        this.resultCount = resultCount;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(String categoryFilter) {
        this.categoryFilter = categoryFilter;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
