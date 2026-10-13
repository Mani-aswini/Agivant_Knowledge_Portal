# Architecture Design Document
## Internal Knowledge Portal with AI Research Agent

### 1. High-Level Architecture Overview

The Internal Knowledge Portal follows a classic **Layered Monolithic Architecture** designed for clarity, maintainability, and ease of demonstration by freshers. It avoids microservices, Kafka, Redis, or external vector databases in favor of a clean, performant, and self-contained system.

```
+-------------------------------------------------------------------------+
|                         Frontend Client (Browser)                       |
|   HTML5 + Vanilla CSS3 + Vanilla JavaScript (Fetch API, No Frameworks)  |
+-------------------------------------------------------------------------+
                                    |  HTTP REST / JSON / Multipart
                                    v
+-------------------------------------------------------------------------+
|                          Presentation Layer                             |
|  - AuthController: Session & credential authentication                  |
|  - KnowledgeDocumentController: Upload, list, details, reindex, archive  |
|  - SearchController: Multi-keyword search & relevance ranking           |
|  - AiAgentController: Goal-driven Q&A with controlled tools             |
|  - FeedbackController: Satisfaction ratings (1-5 stars)                 |
|  - AnalyticsController: Reporting metrics & role simulation             |
|  - GlobalExceptionHandler: Uniform JSON error formatting                |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                            Business Logic Layer                         |
|  - KnowledgeDocumentService: Upload validation, storage, atomicity      |
|  - DocumentParserService: Apache PDFBox & Java UTF-8 text extraction    |
|  - TextChunkerService: Paragraph/window segmentation & section metadata |
|  - SearchService: Term frequency, exact match & title/section scoring   |
|  - PermissionService: Centralized Java clearance rules (indep of AI)    |
|  - FeedbackService & AnalyticsService: Rating records & SQL metrics     |
|  - AiResearchAgent: Goal-driven orchestrator & prompt-injection defense |
|  - KnowledgeAgentTools: 4 controlled Java tools                         |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                            Persistence Layer                            |
|  - Spring Data JPA Repositories (Document, Chunk, Feedback, Search, User)|
|  - Hibernate ORM / Entities                                             |
+-------------------------------------------------------------------------+
                                    |  JDBC (MySQL Connector/J)
                                    v
+-------------------------------------------------------------------------+
|                        Database & Storage Layer                         |
|  - MySQL Server (Port 3306)                                             |
|  - File System: Local 'uploads/' directory for original binaries        |
+-------------------------------------------------------------------------+
```

---

### 2. Layer Responsibilities for Freshers

1. **Controller Layer (`com.example.knowledgeportal.controller`)**:
   - Accepts incoming HTTP requests (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`).
   - Validates input formats (`@Valid`, Bean Validation).
   - Extracts authenticated caller context (`Authentication`).
   - Delegates business logic to services and returns standard HTTP responses (`ResponseEntity`).

2. **Service Layer (`com.example.knowledgeportal.service`, `ai`, `security`)**:
   - Contains all enterprise business rules and security policies.
   - Enforces `@Transactional` boundaries so multi-step operations (e.g. upload file + parse + save document + save 10 chunks) either commit together or roll back cleanly without leaving partial records.
   - Evaluates user clearance levels before returning data or calling the AI model.

3. **Repository Layer (`com.example.knowledgeportal.repository`)**:
   - Interfaces extending `JpaRepository<Entity, Long>`.
   - Spring Data JPA automatically provides standard CRUD methods and generates optimized SQL at runtime.
   - Custom `@Query` methods are used for analytical aggregations (e.g., feedback ratings grouped by document).

4. **Entity Layer (`com.example.knowledgeportal.entity`)**:
   - Plain Java classes mapped to database tables with `@Entity` and `@Table`.
   - Use JPA annotations like `@Id`, `@GeneratedValue`, `@Enumerated(EnumType.STRING)`, and `@Column`.

5. **DTO Layer (`com.example.knowledgeportal.dto`)**:
   - Data Transfer Objects separate internal database structures from client-facing REST API contracts.
   - Prevents leaking sensitive columns (such as password hashes or file paths).

---

### 3. Centralized Permission Service & Access Hierarchy

The application enforces a strict hierarchical access control model in pure Java:

| Role | Permitted Document Clearances |
| :--- | :--- |
| **ROLE_EMPLOYEE** | `PUBLIC`, `EMPLOYEE` |
| **ROLE_HR** | `PUBLIC`, `EMPLOYEE`, `HR` |
| **ROLE_ADMIN** | `PUBLIC`, `EMPLOYEE`, `HR`, `ADMIN` |

#### Key Security Invariant:
**Access clearance is checked in Java before any document content is exposed to search results, citation lists, or supplied as context to an AI model.** The AI model is never trusted to evaluate permissions.

---

### 4. Agentic AI Tool Calling Workflow

Instead of a passive conversational chatbot, the system implements a goal-driven workflow using 4 controlled Java tools:

```
[User Question] 
       │
       ▼
1. User Authentication & Context Extraction (Role + Clearances)
       │
       ▼
2. Tool 1: searchKnowledgeDocuments(query, filters, userContext)
   └── Strictly queries documents within caller's clearance
       │
       ▼
3. Tool 2: retrieveDocumentEvidence(documentId, chunkId, userContext)
   └── Validates document clearance and retrieves top candidate chunks
       │
       ▼
4. Tool 3: validateEvidence(question, retrievedChunks)
   ├── [Insufficient factual overlap] ──► ABSTAIN (return transparent message)
   └── [Sufficient factual overlap]
       │
       ▼
5. Prompt-Injection Neutralizer
   └── Redacts / defuses instructional overrides within untrusted document text
       │
       ▼
6. Answer Synthesis (REAL_AI via API or Deterministic SIMULATION)
   └── Extracts or generates factual answer with citations
       │
       ▼
7. Tool 4: validateCitations(answer, retrievedChunks)
   └── Verifies every citation matches a retrieved chunk
       │
       ▼
[Structured Response (Answer + Citations + Mode + Reasoning)]
```
