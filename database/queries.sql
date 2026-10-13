-- ===================================================================
-- Analytical and Administrative SQL Queries
-- Internal Knowledge Portal with AI Research Agent
-- ===================================================================

USE knowledge_portal_db;

-- -------------------------------------------------------------------
-- Query 1: Documents Grouped by Category and Status
-- Purpose: Provides breakdown of knowledge base composition across categories
-- (POLICY, TECHNICAL_RUNBOOK, FAQ) and lifecycle state (ACTIVE, ARCHIVED).
-- Used by: Dashboard & Analytics Screen
-- -------------------------------------------------------------------
SELECT
    category,
    status,
    COUNT(*) AS total_documents,
    MIN(uploaded_at) AS earliest_upload,
    MAX(updated_at) AS latest_update
FROM knowledge_documents
GROUP BY category, status
ORDER BY category ASC, status ASC;

-- -------------------------------------------------------------------
-- Query 2: Documents Awaiting Re-indexing
-- Purpose: Identifies documents flagged with needs_reindex = TRUE
-- (e.g. after schema change, chunking strategy update, or file modification).
-- Used by: Admin Management Dashboard
-- -------------------------------------------------------------------
SELECT
    id,
    title,
    category,
    owner,
    version,
    updated_at
FROM knowledge_documents
WHERE needs_reindex = TRUE
  AND status = 'ACTIVE'
ORDER BY updated_at DESC;

-- -------------------------------------------------------------------
-- Query 3: Most Frequently Searched Categories
-- Purpose: Aggregates search history to show employee information demand
-- by document category and average search result count.
-- Used by: Analytics Screen (Demand Analysis)
-- -------------------------------------------------------------------
SELECT
    COALESCE(category_filter, 'ALL_CATEGORIES') AS category,
    COUNT(*) AS total_searches,
    ROUND(AVG(result_count), 2) AS avg_results_returned,
    MAX(created_at) AS latest_searched_at
FROM search_history
GROUP BY category_filter
ORDER BY total_searches DESC;

-- -------------------------------------------------------------------
-- Query 4: Feedback Ratings Grouped by Document
-- Purpose: Aggregates user feedback (1 to 5 stars) per document, calculating
-- the total review count, average star rating, and distribution.
-- Used by: Analytics & Content Quality Review
-- -------------------------------------------------------------------
SELECT
    kd.id AS document_id,
    kd.title AS document_title,
    kd.category AS document_category,
    COUNT(kf.id) AS total_feedback_count,
    ROUND(AVG(kf.rating), 2) AS average_rating,
    SUM(CASE WHEN kf.rating = 5 THEN 1 ELSE 0 END) AS five_star_count,
    SUM(CASE WHEN kf.rating <= 2 THEN 1 ELSE 0 END) AS low_rating_count
FROM knowledge_documents kd
INNER JOIN knowledge_feedback kf ON kd.id = kf.document_id
GROUP BY kd.id, kd.title, kd.category
ORDER BY average_rating DESC, total_feedback_count DESC;

-- -------------------------------------------------------------------
-- Query 5: Documents Accessible to a Given Role (e.g., 'ROLE_EMPLOYEE')
-- Purpose: Verifies which active documents can be viewed by a specific role.
-- Rules:
-- - ROLE_EMPLOYEE can view: PUBLIC, EMPLOYEE
-- - ROLE_HR can view: PUBLIC, EMPLOYEE, HR
-- - ROLE_ADMIN can view: PUBLIC, EMPLOYEE, HR, ADMIN
-- -------------------------------------------------------------------
-- Example A: For Employee Role ('ROLE_EMPLOYEE')
SELECT
    id,
    title,
    category,
    access_level,
    version,
    uploaded_at
FROM knowledge_documents
WHERE status = 'ACTIVE'
  AND access_level IN ('PUBLIC', 'EMPLOYEE')
ORDER BY title ASC;

-- Example B: For HR Role ('ROLE_HR')
SELECT
    id,
    title,
    category,
    access_level,
    version,
    uploaded_at
FROM knowledge_documents
WHERE status = 'ACTIVE'
  AND access_level IN ('PUBLIC', 'EMPLOYEE', 'HR')
ORDER BY title ASC;

-- Example C: For Admin Role ('ROLE_ADMIN')
SELECT
    id,
    title,
    category,
    access_level,
    version,
    uploaded_at
FROM knowledge_documents
WHERE status = 'ACTIVE'
ORDER BY title ASC;
