package com.example.knowledgeportal.entity;

/**
 * IndexingStatus Enum
 * 
 * Represents the processing state of a document's search index.
 * 
 * Beginner Concept:
 * In enterprise search systems, uploading a document and processing it
 * (extracting text, generating passages, creating search embeddings)
 * are distinct from its business lifecycle (ACTIVE vs ARCHIVED).
 * Tracking IndexingStatus ensures that documents are only searchable
 * once processing succeeds (INDEXED), and allows tracking in-flight (PENDING)
 * or failed (FAILED) indexing jobs with retry options.
 */
public enum IndexingStatus {
    PENDING,
    INDEXED,
    FAILED
}
