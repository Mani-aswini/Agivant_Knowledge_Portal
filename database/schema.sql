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
    custom_category VARCHAR(100),
    owner VARCHAR(100) NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    indexing_status VARCHAR(50) NOT NULL DEFAULT 'INDEXED',
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
    INDEX idx_doc_indexing (indexing_status),
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
    document_id BIGINT NULL,
    question VARCHAR(500) NOT NULL,
    answer_reference VARCHAR(100) NOT NULL,
    answer_text TEXT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    user_id VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_feedback_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents(id) ON DELETE SET NULL,
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

-- 6. Document Versions table (Historical snapshot audit records)
CREATE TABLE IF NOT EXISTS document_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    document_title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    custom_category VARCHAR(100),
    access_level VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    chunk_count INT NOT NULL,
    change_summary VARCHAR(500),
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_version_doc (document_id),
    INDEX idx_version_number (document_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Audit Logs table (Compliance tracking for security and document lifecycle operations)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id BIGINT NULL,
    outcome VARCHAR(50) NOT NULL,
    details TEXT,
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_actor (actor),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Processing Jobs table (Tracks document ingestion pipeline execution, health, and safe retries)
CREATE TABLE IF NOT EXISTS processing_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NULL,
    document_title VARCHAR(255) NOT NULL,
    document_version INT NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    INDEX idx_job_status (status),
    INDEX idx_job_doc (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Employee Profiles table (Company directory and employee details)
CREATE TABLE IF NOT EXISTS employee_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    date_of_joining DATE NOT NULL,
    location VARCHAR(100) NOT NULL,
    reporting_manager VARCHAR(100),
    clearance_level VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_emp_username (username),
    INDEX idx_emp_dept (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Employee Leave Records table (Planned, Casual, Sick, WFH, and Comp Off balances)
CREATE TABLE IF NOT EXISTS employee_leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    calendar_year INT NOT NULL DEFAULT 2026,
    planned_leaves_total INT NOT NULL DEFAULT 25,
    planned_leaves_taken INT NOT NULL DEFAULT 0,
    planned_leaves_remaining INT NOT NULL DEFAULT 25,
    casual_leaves_total INT NOT NULL DEFAULT 6,
    casual_leaves_taken INT NOT NULL DEFAULT 0,
    casual_leaves_remaining INT NOT NULL DEFAULT 6,
    sick_leaves_total INT NOT NULL DEFAULT 10,
    sick_leaves_taken INT NOT NULL DEFAULT 0,
    sick_leaves_remaining INT NOT NULL DEFAULT 10,
    wfh_allowance_days INT NOT NULL DEFAULT 24,
    wfh_used_days INT NOT NULL DEFAULT 0,
    wfh_remaining_days INT NOT NULL DEFAULT 24,
    comp_off_earned INT NOT NULL DEFAULT 0,
    comp_off_availed INT NOT NULL DEFAULT 0,
    comp_off_balance INT NOT NULL DEFAULT 0,
    approved_leaves_count INT NOT NULL DEFAULT 0,
    pending_leaves_count INT NOT NULL DEFAULT 0,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_leave_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Employee Leave Requests table (Individual leave applications)
CREATE TABLE IF NOT EXISTS employee_leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    employee_name VARCHAR(100) NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_count INT NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_req_user (username),
    INDEX idx_req_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

