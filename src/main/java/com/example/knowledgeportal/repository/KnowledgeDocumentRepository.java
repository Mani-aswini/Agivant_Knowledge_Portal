package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByStatus(DocumentStatus status);

    List<KnowledgeDocument> findByStatusAndAccessLevelIn(DocumentStatus status, Collection<AccessLevel> accessLevels);

    List<KnowledgeDocument> findByStatusAndCategoryAndAccessLevelIn(DocumentStatus status, DocumentCategory category, Collection<AccessLevel> accessLevels);

    long countByStatus(DocumentStatus status);

    long countByNeedsReindexTrueAndStatus(DocumentStatus status);

    List<KnowledgeDocument> findByNeedsReindexTrueAndStatus(DocumentStatus status);

    // SQL Requirement 1: Documents grouped by category and status
    @Query("SELECT d.category, d.status, COUNT(d) FROM KnowledgeDocument d GROUP BY d.category, d.status ORDER BY d.category ASC")
    List<Object[]> countGroupedByCategoryAndStatus();

    // SQL Requirement 5: Documents accessible to a given role's permitted access levels
    @Query("SELECT d FROM KnowledgeDocument d WHERE d.status = :status AND d.accessLevel IN :accessLevels ORDER BY d.title ASC")
    List<KnowledgeDocument> findAccessibleDocuments(@Param("status") DocumentStatus status, @Param("accessLevels") Collection<AccessLevel> accessLevels);
}
