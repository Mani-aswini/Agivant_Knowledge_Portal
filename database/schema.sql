-- ===================================================================
-- Database Schema for Internal Knowledge Portal with AI Research Agent
-- Target Database: MySQL 8.0+ / 26.x
-- ===================================================================

CREATE DATABASE IF NOT EXISTS knowledge_portal_db;
USE knowledge_portal_db;

-- 1. Users table for role-based authentication and document access control
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Knowledge Documents table
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    owner VARCHAR(100) NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    searchable_text LONGTEXT,
    file_storage_path VARCHAR(500),
    uploaded_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    needs_reindex BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at DATETIME NULL,
    INDEX idx_doc_category (category),
    INDEX idx_doc_status (status),
    INDEX idx_doc_access (access_level),
    INDEX idx_doc_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Knowledge Chunks table (stores segmented passages for granular retrieval)
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    document_version INT NOT NULL,
    chunk_index INT NOT NULL,
    section_reference VARCHAR(255),
    page_number INT NULL,
    content MEDIUMTEXT NOT NULL,
    searchable_text MEDIUMTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    INDEX idx_chunk_doc_version (document_id, document_version),
    INDEX idx_chunk_index (chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Knowledge Feedback table
CREATE TABLE IF NOT EXISTS knowledge_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer_reference VARCHAR(100) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    user_id VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_feedback_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    INDEX idx_feedback_doc (document_id),
    INDEX idx_feedback_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Search History table
CREATE TABLE IF NOT EXISTS search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(500) NOT NULL,
    category_filter VARCHAR(50) NULL,
    result_count INT NOT NULL DEFAULT 0,
    user_id VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_search_created (created_at),
    INDEX idx_search_category (category_filter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
