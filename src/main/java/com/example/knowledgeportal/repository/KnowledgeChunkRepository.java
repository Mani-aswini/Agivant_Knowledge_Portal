package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocumentIdAndDocumentVersion(Long documentId, int documentVersion);

    Optional<KnowledgeChunk> findByIdAndDocumentId(Long id, Long documentId);

    void deleteByDocumentIdAndDocumentVersion(Long documentId, int documentVersion);

    void deleteByDocumentId(Long documentId);

    @Query("SELECT c FROM KnowledgeChunk c WHERE c.documentId IN :documentIds")
    List<KnowledgeChunk> findChunksForDocuments(@Param("documentIds") Collection<Long> documentIds);
}
