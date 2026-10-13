package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop10ByOrderByCreatedAtDesc();

    // SQL Requirement 3: Most frequently searched categories
    @Query("SELECT COALESCE(s.categoryFilter, 'ALL_CATEGORIES'), COUNT(s.id), AVG(s.resultCount) " +
           "FROM SearchHistory s GROUP BY s.categoryFilter ORDER BY COUNT(s.id) DESC")
    List<Object[]> findTopSearchedCategories();
}
