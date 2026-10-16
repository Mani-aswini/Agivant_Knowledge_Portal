# AGIVANT KNOWLEDGE PORTAL (Enterprise KG-RAG)
## Comprehensive Technical Architecture & Scenario Documentation

---

### Document Overview
- **Project Name:** Agivant Knowledge Portal (`Enterprise_KG_RAG`)
- **System Classification:** Enterprise Knowledge Management & Retrieval-Augmented Generation (RAG) Platform
- **Target Audience:** Engineering Management, Architecture Review Boards, Technical Leads
- **Document Version:** 1.0.0
- **Status:** Production-Ready / Active

---

## 1. Executive Summary & Business Objectives

### 1.1 Problem Statement
Modern enterprises face severe friction in internal information discovery:
1. **Siloed and Fragmented Data:** Critical documentation, company policies, and operational runbooks are scattered across disconnected formats and directories.
2. **Access Control & Clearance Leakage:** Traditional search tools lack security-clearance awareness, risking internal leaks of confidential HR or executive data to standard employees.
3. **AI Hallucinations & Lack of Accountability:** Standard LLM solutions generate non-verifiable answers without exact corporate citations, creating operational risks.
4. **Disconnected Employee Services:** Employee directory information, department metadata, and leave balances are kept isolated from the central enterprise knowledge base.

### 1.2 Solution & Value Proposition
The **Agivant Knowledge Portal** is an enterprise-grade platform integrating:
- **Zero-Trust Role-Based Access Control (RBAC):** Hierarchical clearance boundaries (`PUBLIC`, `EMPLOYEE`, `HR_ONLY`, `CONFIDENTIAL`).
- **Verifiable RAG AI Engine:** AI responses generated with strict document chunk citations (`[Document Title, Chunk #]`) backed by Google Gemini API and a deterministic extractive synthesis fallback.
- **Automated Ingestion Pipeline:** Automated parsing (.pdf/.txt), semantic chunking, and version tracking.
- **Enterprise Employee & Leave Management:** Corporate identity normalization (`<username>@company.internal`), leave quota allocation, and directory services.
- **Full Operational Observability:** Tamper-evident audit logging, async processing job retries, and real-time system metrics.

---

## 2. Technology Stack & Architectural Blueprint

```
+---------------------------------------------------------------------------------------+
|                                    PRESENTATION LAYER                                 |
|   HTML5 Semantic Structure | Modern Glassmorphism CSS3 | Vanilla ES6 JavaScript (app.js)|
+-------------------------------------------+-------------------------------------------+
                                            | REST API Requests (JSON / Multipart)
                                            v
+---------------------------------------------------------------------------------------+
|                                SECURITY & CONTROLLER LAYER                            |
|   Spring Security (BCrypt, Session Filter) | CustomUserDetailsService | PermissionService|
|   AuthController | SearchController | AiAgentController | KnowledgeDocumentController  |
|   EmployeeController | FeedbackController | AuditLogController | ProcessingJobController|
+-------------------------------------------+-------------------------------------------+
                                            | Validated DTOs
                                            v
+---------------------------------------------------------------------------------------+
|                                 SERVICE & BUSINESS LOGIC                              |
|   AiResearchAgent | SearchService | KnowledgeDocumentService | EmployeeService         |
|   TextChunkerService | DocumentParserService | ProcessingJobService | AuditLogService |
+---------------------+---------------------+---------------------+---------------------+
                      |                     |                     |
                      v                     v                     v
            +-------------------+ +-------------------+ +-------------------+
            |   AI RAG Engine   | | Spring Data JPA   | | Audit & Pipeline  |
            |   Google Gemini   | | Hibernate ORM     | | Ingestion Queue   |
            |   Fallback Synth  | | MySQL Relational  | | Health Diagnostics|
            +-------------------+ +-------------------+ +-------------------+
```

### 2.1 Backend Core
- **Framework:** Spring Boot 3.3.4
- **Language:** Java 21 (LTS)
- **Security:** Spring Security (Stateful HTTP session cookies, BCrypt password hashing, pre-auth permission evaluation)
- **Persistence:** Spring Data JPA with Hibernate ORM
- **Database:** MySQL 8.x relational database
- **Build Tool:** Apache Maven

### 2.2 Artificial Intelligence & RAG Core
- **Primary Inference:** Google Gemini API (`AiClient.java`)
- **Fault-Tolerant Fallback:** Deterministic Extractive Multi-Document Synthesis (`AiResearchAgent.java`)
- **Chunking Engine:** Sliding Window Token/Sentence Chunker (`TextChunkerService.java`)
- **Document Text Extraction:** Apache PDFBox & standard stream parsers (`DocumentParserService.java`)
- **Citation Extraction:** Source attribution mapping (`CitationSource.java`, `EvidenceChunk.java`)

### 2.3 Frontend Core
- **Structure:** Semantic HTML5 (`index.html`)
- **Styling:** Vanilla CSS3 (`styles.css`) with CSS custom properties, dark/light themes, glassmorphism, responsive grid layouts, and slide-out navigation sitemap.
- **Application Logic:** Modular Vanilla JavaScript (`app.js`) utilizing `fetch` async/await, DOM state management, dynamic role routing, and real-time form validations.

---

## 3. User Personas & Role-Based Access Control (RBAC)

The system enforces a two-dimensional authorization model: **User Role** $\times$ **Document Access Level**.

### 3.1 User Roles
| Role | Identifier | Responsibilities & UI Privileges |
| :--- | :--- | :--- |
| **Employee** | `ROLE_EMPLOYEE` | Standard staff access: Search documents, chat with AI Assistant, view personal profile and leave balance, view sanitized employee directory. |
| **HR Specialist** | `ROLE_HR` | Human resources personnel: All Employee permissions + upload HR documents, manage employee directory, allocate leave balances, view user feedback. |
| **System Admin** | `ROLE_ADMIN` | Global administrator: Full system access + document archive/delete, user account administration, audit logs review, ingestion pipeline diagnostics & retries. |

### 3.2 Document Clearance Hierarchy
Documents and their generated knowledge chunks are tagged with an `AccessLevel`:
1. `PUBLIC`: Accessible by all authenticated users and external public inquiries.
2. `EMPLOYEE`: Accessible by users with `ROLE_EMPLOYEE`, `ROLE_HR`, or `ROLE_ADMIN`.
3. `HR_ONLY`: Accessible exclusively by `ROLE_HR` and `ROLE_ADMIN`.
4. `CONFIDENTIAL`: Accessible exclusively by `ROLE_ADMIN`.

---

## 4. End-to-End Functional Scenarios (Step-by-Step)

---

### Scenario 1: User Authentication & Dynamic Role Landing

#### 1.1 Business Objective
Allow corporate users to securely authenticate via credentials, enforce enterprise username normalization, create a secure session, and dynamically route the user to their designated workspace (Employee Workspace vs. Administrator Command Center).

#### 1.2 Step-by-Step Execution Flow
```
[User Form Click]
       |
       v
1. HTML (index.html): User submits #loginForm with username and password.
       |
       v
2. JavaScript (app.js): handleLogin() intercepts submit, captures inputs, trims username,
   and POSTs JSON to /api/auth/login.
       |
       v
3. DTO (LoginRequest.java): Captures { username, password } with @NotBlank validations.
       |
       v
4. Controller (AuthController.java): login() endpoint receives request.
       |
       v
5. Security AuthenticationManager (SecurityConfig.java): Invokes CustomUserDetailsService.
       |
       v
6. Service (CustomUserDetailsService.java):
   - Executes case-insensitive lookup: userRepository.findByUsernameIgnoreCase(rawUsername).
   - Validates user exists and account is enabled.
       |
       v
7. Password Verification: BCryptPasswordEncoder compares raw password with hashed password.
       |
       v
8. Session Establishment: Spring Security writes SecurityContext to the HTTP session;
   JSESSIONID cookie returned to browser.
       |
       v
9. Audit Logging (AuditLogService.java): Records action 'LOGIN' for user in database.
       |
       v
10. DTO Response (UserResponse.java): Returns { id, username, email, role, department }.
       |
       v
11. UI Routing (app.js):
    - Stores currentUser in memory.
    - If role === 'ROLE_EMPLOYEE': Renders #employeeDashboardView (Quick Links, Categories, Leave Card).
    - If role === 'ROLE_ADMIN' or 'ROLE_HR': Renders #adminDashboardView (System KPIs, Ingestion Stats, Health).
```

#### 1.3 Key Files Involved
- [index.html](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/resources/static/index.html)
- [app.js](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/resources/static/js/app.js)
- [LoginRequest.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/dto/LoginRequest.java)
- [AuthController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/AuthController.java)
- [CustomUserDetailsService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/security/CustomUserDetailsService.java)
- [UserAccountRepository.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/repository/UserAccountRepository.java)
- [SecurityConfig.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/security/SecurityConfig.java)

---

### Scenario 2: Enterprise Knowledge Search Engine

#### 2.1 Business Objective
Provide instant, keyword- and relevance-scored retrieval across indexed documents and semantic chunks while strictly preventing unauthorized clearance leakage.

#### 2.2 Step-by-Step Execution Flow
```
[User Search Click / Input]
       |
       v
1. HTML (index.html): User enters keyword in #searchKeywordInput and clicks #executeSearchBtn.
       |
       v
2. JavaScript (app.js): executeSearch() constructs SearchRequest payload:
   { query, categoryFilter, accessLevelFilter, limit: 10 } and calls POST /api/knowledge/search.
       |
       v
3. Controller (SearchController.java): Receives SearchRequest and caller Authentication.
       |
       v
4. Security Clearance Evaluation (PermissionService.java):
   - Computes caller's allowed access levels (e.g., Employee -> [PUBLIC, EMPLOYEE]).
       |
       v
5. Service (SearchService.java):
   - Queries KnowledgeDocumentRepository & KnowledgeChunkRepository.
   - Applies lexical matching, title weighting, and chunk relevance scoring.
   - Filters out any document/chunk above caller's permitted clearance.
       |
       v
6. Persistence (SearchHistoryRepository.java):
   - Records query text, caller username, execution timestamp, and result count in search_history.
       |
       v
7. DTO Response (SearchResponse.java):
   - Serializes List<SearchResultItem> with title, excerpt, category, clearance, and relevanceScore.
       |
       v
8. UI Rendering (app.js):
   - Clears spinner and populates #searchResultsContainer with interactive result cards.
```

#### 2.3 Key Files Involved
- [SearchController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/SearchController.java)
- [SearchService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/SearchService.java)
- [PermissionService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/security/PermissionService.java)
- [SearchHistory.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/entity/SearchHistory.java)

---

### Scenario 3: RAG AI Research Assistant with Traceable Citations

#### 3.1 Business Objective
Enable employees to ask natural language questions and receive accurate, context-grounded answers generated from verified internal documents. Every answer must provide direct citation sources, maintain chat history, and allow user feedback.

#### 3.2 Step-by-Step Execution Flow
```
[User Chat Click / Enter]
       |
       v
1. HTML (index.html): User types prompt into #aiAgentInput and clicks #aiAgentSendBtn.
       |
       v
2. JavaScript (app.js): submitAiAgentQuery() displays user message in chat stream,
   renders typing skeleton loader, and POSTs to /api/knowledge/ask.
       |
       v
3. DTO (AskRequest.java): Contains { question, conversationId, preferredCategory }.
       |
       v
4. Controller (AiAgentController.java): Receives request with SecurityContext.
       |
       v
5. Service (AiResearchAgent.java):
   a. Clearance Determination: Invokes PermissionService to establish security boundary.
   b. Knowledge Retrieval: Invokes KnowledgeAgentTools to fetch candidate chunks matching the question.
   c. Re-ranking: Computes chunk overlap and relevance; retains top high-confidence chunks.
   d. Multi-Tier Generation:
      - Primary Tier: Invokes AiClient.java (Google Gemini API) passing system prompt + context + question.
      - Secondary Tier (Fallback): If external API is unreachable or rate-limited, executes
        deterministic sentence extraction and semantic synthesis directly from chunks.
   e. Citation Building: Builds List<CitationSource> containing documentId, title, chunkIndex, and excerpt.
       |
       v
6. Persistence (ChatMessageRepository.java):
   - Stores user prompt and AI response linked to conversationId and username in chat_message.
       |
       v
7. DTO Response (AskResponse.java):
   - Returns { answerText, citations, confidenceScore, conversationId, generatedTimestamp }.
       |
       v
8. UI Rendering (app.js):
   - Removes typing indicator, renders markdown-formatted response.
   - Renders clickable citation badges and Thumbs Up / Down feedback buttons.
   - Refreshes right-hand sitemap chat drawer history.
```

#### 3.3 Key Files Involved
- [AiAgentController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/AiAgentController.java)
- [AiResearchAgent.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/ai/AiResearchAgent.java)
- [AiClient.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/ai/AiClient.java)
- [KnowledgeAgentTools.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/ai/KnowledgeAgentTools.java)
- [ChatMessage.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/entity/ChatMessage.java)

---

### Scenario 4: Document Ingestion, Parsing & Chunking Pipeline

#### 4.1 Business Objective
Allow administrators and HR specialists to upload corporate documents (.pdf, .txt, markdown), automatically extract raw text, segment content into searchable semantic chunks, track indexing status, and manage document lifecycles (re-index, archive, delete).

#### 4.2 Step-by-Step Execution Flow
```
[Admin/HR Document Upload Click]
       |
       v
1. HTML (index.html): User fills #documentUploadForm (title, category, accessLevel, file attachment)
   and clicks #uploadDocSubmitBtn.
       |
       v
2. JavaScript (app.js): uploadDocument() builds multipart/form-data payload and sends
   POST /api/knowledge/documents.
       |
       v
3. Controller (KnowledgeDocumentController.java): uploadDocument() receives MultipartFile.
       |
       v
4. Service (KnowledgeDocumentService.java):
   a. Validates file extension (.pdf, .txt, .md) and file size.
   b. Extracts raw text via DocumentParserService (Apache PDFBox for PDF, stream reader for TXT).
   c. Creates KnowledgeDocument record with IndexingStatus.PENDING and creates DocumentVersion v1.
   d. Creates ProcessingJob record in database (JobType.INGESTION, status IN_PROGRESS).
       |
       v
5. Chunking (TextChunkerService.java):
   - Segments raw text into structured chunks (target: 300-500 words with 50-word overlap).
   - Generates KnowledgeChunk entities with chunkIndex and preview text.
       |
       v
6. Persistence:
   - Saves all KnowledgeChunk records in knowledge_chunk.
   - Updates KnowledgeDocument status to IndexingStatus.COMPLETED.
   - Updates ProcessingJob status to SUCCESS.
   - Logs event in audit_log.
       |
       v
7. UI Response:
   - Document table refreshes immediately with new entry, chunk count, and badge.
```

#### 4.3 Key Files Involved
- [KnowledgeDocumentController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/KnowledgeDocumentController.java)
- [KnowledgeDocumentService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/KnowledgeDocumentService.java)
- [DocumentParserService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/DocumentParserService.java)
- [TextChunkerService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/TextChunkerService.java)
- [ProcessingJobService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/ProcessingJobService.java)

---

### Scenario 5: Corporate Employee & Leave Management

#### 5.1 Business Objective
Maintain a directory of enterprise employees, enforce mandatory corporate email formatting (`<prefix>@company.internal`), and manage leave quotas (casual, sick, earned leaves) with individual balance tracking.

#### 5.2 Step-by-Step Execution Flow
```
[Admin/HR Add Employee Click]
       |
       v
1. HTML (index.html): User enters full name, username, department, job title, and company email.
   Email field displays permanent visual suffix '@company.internal'.
       |
       v
2. JavaScript (app.js):
   - Normalizes email prefix: only allows letters, numbers, dots, and hyphens.
   - Strips any duplicate @domain input by user.
   - Assembles full corporate email: username + "@company.internal".
   - Sends POST /api/employees with CreateEmployeeRequest.
       |
       v
3. DTO Validation (CreateEmployeeRequest.java):
   - Validates @Pattern(regexp = "^[A-Za-z0-9._%+-]+@company\\.internal$").
   - Validates @NotBlank for username, fullName, department, and role.
       |
       v
4. Controller (EmployeeController.java): createEmployee() invokes EmployeeService.
       |
       v
5. Service (EmployeeService.java):
   a. Normalizes username to lowercase.
   b. Verifies no existing user holds the username or email.
   c. Creates UserAccount with default hashed password and requested UserRole.
   d. Creates EmployeeProfile with contact details and joining date.
   e. Initializes EmployeeLeaveRecord with enterprise annual leave quota:
      - Casual Leave: 12 allocated, 0 used, 12 remaining.
      - Sick Leave: 10 allocated, 0 used, 10 remaining.
      - Earned Leave: 15 allocated, 0 used, 15 remaining.
   f. Records AuditLog entry 'CREATE_EMPLOYEE'.
       |
       v
6. UI Rendering:
   - Refreshes employee directory table with new employee badge, department, and corporate email.
```

#### 5.3 Key Files Involved
- [EmployeeController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/EmployeeController.java)
- [EmployeeService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/EmployeeService.java)
- [CreateEmployeeRequest.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/dto/CreateEmployeeRequest.java)
- [EmployeeProfile.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/entity/EmployeeProfile.java)
- [EmployeeLeaveRecord.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/entity/EmployeeLeaveRecord.java)

---

### Scenario 6: System Administration, Audit Logs & Pipeline Diagnostics

#### 6.1 Business Objective
Provide system administrators with full visibility into platform operations, user actions, document processing job health, and the ability to re-trigger failed ingestion jobs.

#### 6.2 Step-by-Step Execution Flow
```
[Admin Clicks Ingestion Jobs / Audit Logs]
       |
       v
1. HTML (index.html): Admin navigates to 'Audit Trail' or 'Processing Pipeline' tab.
       |
       v
2. JavaScript (app.js):
   - Invokes GET /api/knowledge/audit-logs?limit=50
   - Invokes GET /api/knowledge/processing-jobs
       |
       v
3. Controllers:
   - AuditLogController.java returns historical event log entries.
   - ProcessingJobController.java returns list of asynchronous background jobs.
       |
       v
4. Job Retry Action:
   - If a job shows status 'FAILED', admin clicks 'Retry'.
   - JavaScript sends POST /api/knowledge/processing-jobs/{id}/retry.
   - ProcessingJobService reloads the file, resets status to 'IN_PROGRESS',
     and re-executes parsing and chunking.
       |
       v
5. UI Response:
   - Status badge transitions from 'FAILED' -> 'IN_PROGRESS' -> 'SUCCESS'.
```

#### 6.3 Key Files Involved
- [AuditLogController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/AuditLogController.java)
- [AuditLogService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/AuditLogService.java)
- [ProcessingJobController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/ProcessingJobController.java)
- [ProcessingJobService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/ProcessingJobService.java)
- [AnalyticsController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/AnalyticsController.java)

---

### Scenario 7: User Feedback Collection & Portal Quality Analytics

#### 7.1 Business Objective
Empower enterprise employees to rate AI-generated answers and retrieved knowledge on a 1–5 star scale with contextual qualitative comments, while providing HR Specialists and System Administrators with centralized quality analytics, real-time satisfaction score KPIs, star-rating distribution breakdowns, and an auditable review dashboard with document cross-referencing.

#### 7.2 Step-by-Step Execution Flow
```
[User Submits Star Rating & Review]
       |
       v
1. HTML (index.html):
   - User clicks star rating (1–5) or '#btnWriteFeedback' on AI assistant response bubble.
   - Feedback modal (#feedbackModal) opens with pre-populated question & answer citation.
       |
       v
2. JavaScript (app.js):
   - submitFeedback() validates rating input (1 to 5) and constructs JSON payload:
     { documentId, question, answerReference, rating, comment, answerText }
   - Dispatches POST /api/knowledge/feedback.
       |
       v
3. Controller (FeedbackController.java):
   - submitFeedback() validates payload using JSR-380 (@Min(1), @Max(5), @NotBlank).
   - Injects Spring Security Authentication context and forwards to FeedbackService.
       |
       v
4. Security & Access Control (PermissionService.java):
   - Resolves authenticated principal (username).
   - If documentId is present, executes validateDocumentAccess(auth, document) to prevent
     clearance circumvention across confidential policies.
       |
       v
5. Service Persistence & Audit (FeedbackService.java & AuditLogService.java):
   - Instantiates KnowledgeFeedback entity and persists to MySQL via KnowledgeFeedbackRepository.
   - Logs audit trail event ('SUBMIT_FEEDBACK') with rating and document metadata.
   - Returns FeedbackResponse with confirmation message.
       |
       v
6. HR & Admin Analytics Review Flow (Screen G):
   - HR or Admin navigates to 'Feedback Analytics' (#tabFeedback in navigation sitemap).
   - app.js invokes GET /api/knowledge/feedback?rating={filter}&search={query}.
   - FeedbackService.getFeedbackManagement():
     * Enforces ROLE_HR or ROLE_ADMIN clearance via PermissionService.validateAdminOrHr().
     * Calculates total submissions, average satisfaction score (rounded to 1 decimal).
     * Computes 1-to-5 star rating distribution map.
     * Enriches feedback records with document title, category, and version numbers.
     * Returns FeedbackManagementResponse.
       |
       v
7. UI Rendering (app.js):
   - Renders overall average rating score card (#feedbackAvgScore) and total feedback count.
   - Visualizes proportional star distribution progress bars (#starDist5 to #starDist1).
   - Populates historical feedback review table with star badges, user IDs, questions, and comments.
```

#### 7.3 Key Files Involved
- [index.html](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/resources/static/index.html)
- [app.js](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/resources/static/js/app.js)
- [FeedbackController.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/controller/FeedbackController.java)
- [FeedbackService.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/service/FeedbackService.java)
- [KnowledgeFeedbackRepository.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/repository/KnowledgeFeedbackRepository.java)
- [KnowledgeFeedback.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/entity/KnowledgeFeedback.java)
- [FeedbackRequest.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/dto/FeedbackRequest.java)
- [FeedbackResponse.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/dto/FeedbackResponse.java)
- [FeedbackManagementResponse.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/dto/FeedbackManagementResponse.java)
- [FeedbackAdminItemResponse.java](file:///c:/Users/AswiniMani/Desktop/Enterprise_KG_RAG/src/main/java/com/example/knowledgeportal/dto/FeedbackAdminItemResponse.java)

---

## 5. Complete REST API Catalogue Reference

| Module | HTTP Method | Endpoint URI | Permitted Roles | Request Payload | Response DTO |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/auth/login` | `ALL (PermitAll)` | `LoginRequest` | `UserResponse` |
| **Auth** | `POST` | `/api/auth/register` | `ALL (PermitAll)` | `RegisterRequest` | `UserResponse` |
| **Auth** | `POST` | `/api/auth/logout` | `Authenticated` | None | Status message |
| **Auth** | `GET` | `/api/auth/me` | `Authenticated` | None | `UserResponse` |
| **Search** | `POST` | `/api/knowledge/search` | `Authenticated` | `SearchRequest` | `SearchResponse` |
| **Search** | `GET` | `/api/knowledge/search/history` | `Authenticated` | None | List of recent queries |
| **AI Agent** | `POST` | `/api/knowledge/ask` | `Authenticated` | `AskRequest` | `AskResponse` |
| **AI Agent** | `GET` | `/api/knowledge/chat-history` | `Authenticated` | Query params | Chat message list |
| **Feedback** | `POST` | `/api/knowledge/feedback` | `Authenticated` | `FeedbackRequest` | `FeedbackResponse` |
| **Feedback** | `GET` | `/api/knowledge/feedback` | `ROLE_ADMIN, ROLE_HR` | None | `FeedbackManagementResponse` |
| **Documents**| `GET` | `/api/knowledge/documents` | `Authenticated` | Query params | List of `DocumentResponse` |
| **Documents**| `POST` | `/api/knowledge/documents` | `ROLE_ADMIN, ROLE_HR` | Multipart form | `DocumentResponse` |
| **Documents**| `DELETE`| `/api/knowledge/documents/{id}` | `ROLE_ADMIN` | None | Deletion status |
| **Documents**| `PUT` | `/api/knowledge/documents/{id}/archive` | `ROLE_ADMIN` | None | Updated document |
| **Employees**| `GET` | `/api/employees` | `Authenticated` | None | List of employees |
| **Employees**| `POST` | `/api/employees` | `ROLE_ADMIN, ROLE_HR` | `CreateEmployeeRequest`| `EmployeeDetailResponse` |
| **Employees**| `GET` | `/api/employees/me` | `Authenticated` | None | `EmployeeDetailResponse` |
| **Audit** | `GET` | `/api/knowledge/audit-logs` | `ROLE_ADMIN` | Query params | List of `AuditLog` |
| **Jobs** | `GET` | `/api/knowledge/processing-jobs` | `ROLE_ADMIN` | None | List of `ProcessingJob` |
| **Jobs** | `POST` | `/api/knowledge/processing-jobs/{id}/retry` | `ROLE_ADMIN` | None | Updated `ProcessingJob` |
| **Analytics**| `GET` | `/api/knowledge/analytics` | `ROLE_ADMIN, ROLE_HR` | None | `AnalyticsResponse` |

---

## 6. Database Entity-Relationship (ER) Schema Overview

```
+------------------+         1:1         +-------------------+
|   user_account   | ------------------- |  employee_profile |
+------------------+                     +-------------------+
| id (PK)          |                     | id (PK)           |
| username (UNIQUE)|                     | user_id (FK)      |
| password_hash    |                     | full_name         |
| email (UNIQUE)   |                     | department        |
| role             |                     | job_title         |
| enabled          |                     +-------------------+
+------------------+                               | 1:N
         |                                         v
         | 1:N                           +-----------------------+
         |                               | employee_leave_record |
         v                               +-----------------------+
+------------------+                     | id (PK)               |
|    audit_log     |                     | employee_id (FK)      |
+------------------+                     | leave_type            |
| id (PK)          |                     | total_allocated       |
| username         |                     | used_days             |
| action           |                     | remaining_days        |
| timestamp        |                     +-----------------------+
+------------------+

+--------------------+         1:N         +-------------------+
| knowledge_document | ------------------- |  knowledge_chunk  |
+--------------------+                     +-------------------+
| id (PK)            |                     | id (PK)           |
| title              |                     | document_id (FK)  |
| category           |                     | chunk_index       |
| access_level       |                     | chunk_text        |
| indexing_status    |                     | token_count       |
| created_at         |                     +-------------------+
+--------------------+
         | 1:N                             +--------------------+
         |-------------------------------- | knowledge_feedback |
         |                                 +--------------------+
         |                                 | id (PK)            |
         v                                 | document_id (FK)   |
+--------------------+                     | user_id            |
|  document_version  |                     | rating (1-5)       |
+--------------------+                     | question           |
| id (PK)            |                     | answer_reference   |
| document_id (FK)   |                     | comment            |
| version_number     |                     | answer_text        |
| file_hash          |                     | created_at         |
+--------------------+                     +--------------------+
```

---

## 7. Key Enterprise Security & Quality Assurance Controls

1. **Case-Insensitive Identity Resolution:**
   - User authentication performs `findByUsernameIgnoreCase()` ensuring login resilience while strictly enforcing case sensitivity on password hashes.
2. **Standardized Corporate Email Policy:**
   - Enforces the pattern `^[A-Za-z0-9._%+-]+@company\\.internal$` at both client (HTML/JS) and server (JSR-380 `@Pattern` in `CreateEmployeeRequest.java`), preventing rogue domains.
3. **Database-Level Multi-Tenant Clearance Isolation:**
   - Document chunks are filtered inside SQL queries matching the authenticated principal's clearance level. Standard users cannot retrieve `HR_ONLY` or `CONFIDENTIAL` content even by manipulating API IDs.
4. **Verifiable Citations:**
   - AI answers cannot generate unsupported text without returning source metadata (`documentId`, `chunkIndex`, `title`).
5. **Comprehensive Automated Test Suite:**
   - Verified automated test coverage in `KnowledgeSearchAndSecurityTests.java` and `AiResearchAgentTests.java` confirming zero authorization leakage and high-accuracy RAG synthesis.
