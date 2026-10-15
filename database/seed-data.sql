-- ===================================================================
-- Seed Data for Internal Knowledge Portal with AI Research Agent
-- ===================================================================

USE knowledge_portal_db;

-- 1. Default Users (Pass: password123)
-- BCrypt hash for 'password123' is $2a$10$7Z8l8fA5a7gJjU6Z9/Yy2uJ0nU7T2Y.HqWzM8J7t4gK8W8V1j2K.G
-- (Spring Boot ApplicationRunner also ensures these users exist with fresh hashing if missing)
INSERT IGNORE INTO users (id, username, password, full_name, role, enabled, created_at) VALUES
(1, 'employee1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Alice Employee', 'ROLE_EMPLOYEE', TRUE, NOW()),
(2, 'hr_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Suganthi Mani', 'ROLE_HR', TRUE, NOW()),
(3, 'admin_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Ashwin Mani', 'ROLE_ADMIN', TRUE, NOW());

-- 2. Seed Approved Knowledge Documents
INSERT IGNORE INTO knowledge_documents (id, title, description, category, owner, access_level, version, status, original_file_name, content_type, searchable_text, file_storage_path, uploaded_at, updated_at, needs_reindex, archived_at) VALUES
(1, 'Company Annual Leave and Time Off Policy', 'Comprehensive annual leave guidelines, statutory sick pay, and bereavement rules.', 'POLICY', 'hr_user', 'EMPLOYEE', 1, 'ACTIVE', 'annual_leave_policy.txt', 'text/plain', 'Company Annual Leave and Time Off Policy. Full-time employees are entitled to 25 days of paid annual leave per calendar year. Leave must be approved 2 weeks in advance. Employees can carry over up to 5 unused leave days into the subsequent calendar year. Sick leave provides up to 10 paid days annually with a doctor note required after 3 consecutive days.', NULL, NOW(), NOW(), FALSE, NULL),

(2, 'Production Incident Severity Runbook', 'Operational instructions for diagnosing and handling Severity 1 and Severity 2 system outages.', 'TECHNICAL_RUNBOOK', 'admin_user', 'EMPLOYEE', 1, 'ACTIVE', 'incident_runbook.txt', 'text/plain', 'Production Incident Severity Runbook. Severity 1 incident requires an immediate war room within 10 minutes. The primary on-call engineer must notify stakeholders via the incident Slack channel. Incident resolution time objective is under 60 minutes. Root cause analysis (RCA) document must be submitted within 48 hours following resolution.', NULL, NOW(), NOW(), FALSE, NULL),

(3, 'HR Executive Compensation and Bonus Structure', 'Confidential executive performance appraisal metrics, stock options, and bonus brackets.', 'POLICY', 'hr_user', 'HR', 1, 'ACTIVE', 'executive_compensation.txt', 'text/plain', 'HR Executive Compensation and Bonus Structure. Confidential document for HR and Senior Executives only. Annual performance bonuses are calculated between 15% and 35% of base salary depending on corporate EBITDA targets. Discretionary retention bonuses require HR Director approval.', NULL, NOW(), NOW(), FALSE, NULL),

(4, 'Admin Infrastructure Credentials and Bastion Host Protocols', 'Super-admin access, bastion host SSH gateway instructions, and disaster recovery procedures.', 'TECHNICAL_RUNBOOK', 'admin_user', 'ADMIN', 1, 'ACTIVE', 'admin_bastion_protocols.txt', 'text/plain', 'Admin Infrastructure Credentials and Bastion Host Protocols. Strictly restricted to system administrators. Root bastion host requires hardware MFA token authentication. Direct SSH access to database nodes is strictly prohibited from external networks.', NULL, NOW(), NOW(), FALSE, NULL),

(5, 'Remote Work Equipment and Expense FAQs', 'Frequently asked questions regarding monitor allowances, remote work stipend, and expense reports.', 'FAQ', 'hr_user', 'PUBLIC', 1, 'ACTIVE', 'remote_work_faq.txt', 'text/plain', 'Remote Work Equipment and Expense FAQs. The company reimburses a one-time home office setup allowance of up to $500 for ergonomic chairs and external monitors. Monthly broadband internet stipend is $50 reimbursed via Expensify on the 25th of each month.', NULL, NOW(), NOW(), FALSE, NULL);

-- 3. Seed Knowledge Chunks
INSERT IGNORE INTO knowledge_chunks (id, document_id, document_version, chunk_index, section_reference, page_number, content, searchable_text, created_at) VALUES
-- Document 1 Chunks (Leave Policy)
(1, 1, 1, 1, 'Section 1: Annual Leave Entitlement', 1, 'Full-time regular employees are entitled to 25 days of paid annual leave per calendar year. Leave must be requested through the HR portal at least 2 weeks in advance of the planned start date.', 'full-time regular employees are entitled to 25 days of paid annual leave per calendar year. leave must be requested through the hr portal at least 2 weeks in advance of the planned start date.', NOW()),
(2, 1, 1, 2, 'Section 2: Carry-Over Policy', 1, 'Employees are permitted to carry over a maximum of 5 unused annual leave days into the subsequent calendar year. Carried-over days must be utilized before March 31st of the new year.', 'employees are permitted to carry over a maximum of 5 unused annual leave days into the subsequent calendar year. carried-over days must be utilized before march 31st of the new year.', NOW()),
(3, 1, 1, 3, 'Section 3: Sick Leave Regulations', 2, 'Employees receive up to 10 days of paid sick leave annually. A certified medical practitioner note is mandatory for consecutive absences exceeding 3 days.', 'employees receive up to 10 days of paid sick leave annually. a certified medical practitioner note is mandatory for consecutive absences exceeding 3 days.', NOW()),

-- Document 2 Chunks (Incident Runbook)
(4, 2, 1, 1, 'Section 1: Severity 1 Protocol', 1, 'A Severity 1 incident indicates complete service downtime. The on-call Site Reliability Engineer must initiate a virtual war room within 10 minutes of alert trigger and notify stakeholders.', 'a severity 1 incident indicates complete service downtime. the on-call site reliability engineer must initiate a virtual war room within 10 minutes of alert trigger and notify stakeholders.', NOW()),
(5, 2, 1, 2, 'Section 2: Post-Mortem and RCA', 2, 'Following incident resolution, a comprehensive Root Cause Analysis (RCA) document must be drafted and published to the engineering knowledge base within 48 hours.', 'following incident resolution, a comprehensive root cause analysis (rca) document must be drafted and published to the engineering knowledge base within 48 hours.', NOW()),

-- Document 3 Chunks (HR Compensation - Restricted to HR and ADMIN)
(6, 3, 1, 1, 'Section 1: Bonus Calculation', 1, 'Executive annual performance bonuses are tiered from 15% to 35% of base salary, contingent upon audited corporate EBITDA benchmarks and department key performance indicators.', 'executive annual performance bonuses are tiered from 15% to 35% of base salary, contingent upon audited corporate ebitda benchmarks and department key performance indicators.', NOW()),

-- Document 4 Chunks (Admin Bastion - Restricted to ADMIN)
(7, 4, 1, 1, 'Section 1: Bastion Access', 1, 'Super-admin bastion access requires hardware FIDO2 token verification. All shell sessions are logged to an immutable compliance bucket.', 'super-admin bastion access requires hardware fido2 token verification. all shell sessions are logged to an immutable compliance bucket.', NOW()),

-- Document 5 Chunks (Remote Work FAQ - PUBLIC)
(8, 5, 1, 1, 'Section 1: Hardware Allowance', 1, 'Eligible remote and hybrid employees can claim up to $500 for home office equipment including monitors, keyboard, and ergonomic chairs upon submitting receipts.', 'eligible remote and hybrid employees can claim up to $500 for home office equipment including monitors, keyboard, and ergonomic chairs upon submitting receipts.', NOW()),
(9, 5, 1, 2, 'Section 2: Internet Stipend', 1, 'A monthly internet allowance of $50 is credited directly via payroll upon submitting monthly utility statements.', 'a monthly internet allowance of $50 is credited directly via payroll upon submitting monthly utility statements.', NOW());

-- 4. Seed Feedback Records
INSERT IGNORE INTO knowledge_feedback (id, document_id, question, answer_reference, rating, comment, user_id, created_at) VALUES
(1, 1, 'How many days of annual leave are available?', 'ANS-POLICY-001', 5, 'Clear and accurate citation of the 25 days policy.', 'employee1', NOW()),
(2, 5, 'What is the home office allowance limit?', 'ANS-FAQ-002', 4, 'Helpful answer regarding the $500 equipment reimbursement.', 'employee1', NOW());

-- 5. Seed Search History
INSERT IGNORE INTO search_history (id, query, category_filter, result_count, user_id, created_at) VALUES
(1, 'annual leave days', 'POLICY', 3, 'employee1', NOW()),
(2, 'incident war room', 'TECHNICAL_RUNBOOK', 1, 'employee1', NOW()),
(3, 'remote work monitor', 'FAQ', 2, 'employee1', NOW());
