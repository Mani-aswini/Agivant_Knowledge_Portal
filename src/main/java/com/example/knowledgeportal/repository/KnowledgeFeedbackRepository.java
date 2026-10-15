package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.KnowledgeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeFeedbackRepository extends JpaRepository<KnowledgeFeedback, Long> {

    List<KnowledgeFeedback> findByDocumentId(Long documentId);

    List<KnowledgeFeedback> findAllByOrderByCreatedAtDesc();

    long countByRating(int rating);

    @Query("SELECT AVG(f.rating) FROM KnowledgeFeedback f")
    Double getOverallAverageRating();

    // SQL Requirement 4: Feedback ratings grouped by document
    @Query("SELECT f.documentId, COUNT(f.id), AVG(f.rating), " +
           "SUM(CASE WHEN f.rating = 5 THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN f.rating <= 2 THEN 1L ELSE 0L END) " +
           "FROM KnowledgeFeedback f WHERE f.documentId IS NOT NULL GROUP BY f.documentId")
    List<Object[]> findFeedbackStatsGroupedByDocument();
}

