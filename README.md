# Internal Knowledge Portal with AI Research Agent

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B%20%2F%2026.x-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-15%20Passed-success.svg)]()

A complete, beginner-friendly enterprise web application where employees can upload, manage, search, and retrieve information from approved policy documents, technical runbooks, and frequently asked questions. 

Features an **AI Research Agent** that operates through controlled Java tools to retrieve verifiable document passages, answer natural language inquiries, cite exact source sections, and transparently abstain when evidence is insufficient.

---

## Table of Contents
1. [Project Introduction](#1-project-introduction)
2. [Key Features](#2-key-features)
3. [Technology Stack](#3-technology-stack)
4. [Architecture and Request Flow](#4-architecture-and-request-flow)
5. [Database Schema and Relationships](#5-database-schema-and-relationships)
6. [Prerequisites](#6-prerequisites)
7. [Java and Maven Setup](#7-java-and-maven-setup)
8. [MySQL Server Setup (Server vs. Workbench)](#8-mysql-server-setup)
9. [Database Creation and Migrations](#9-database-creation-and-migrations)
10. [Environment Configuration](#10-environment-configuration)
11. [How to Run the Backend](#11-how-to-run-the-backend)
12. [How to Access the Frontend](#12-how-to-access-the-frontend)
13. [How to Upload Sample Documents](#13-how-to-upload-sample-documents)
14. [How to Test Search and AI Question Answering](#14-how-to-test-search-and-ai-question-answering)
15. [How to Configure Real AI Access (OpenAI / Spring AI)](#15-how-to-configure-real-ai-access)
16. [How to Run Automated Unit and Integration Tests](#16-how-to-run-automated-unit-and-integration-tests)
17. [How to Use Git](#17-how-to-use-git)
18. [Known Limitations](#18-known-limitations)
19. [Security and Prompt-Injection Defense Design](#19-security-and-prompt-injection-defense-design)
20. [Troubleshooting Common Errors](#20-troubleshooting-common-errors)
21. [Extra Technologies Used Beyond Mandatory Stack](#21-extra-technologies-used-beyond-mandatory-stack)
22. [Final Technology Inventory (Mandatory)](#22-final-technology-inventory)

---

## 1. Project Introduction

In many organizations, critical knowledge is scattered across shared drives, emails, and PDFs. When employees search for information or query an AI model:
- Generic chatbots frequently hallucinate unsupported policies.
- Standard vector databases often lack fine-grained, role-based security filters.
- Confidential executive or administrative files risk being exposed to unauthorized users.

This project solves these challenges by combining:
- A clean **Spring Boot 3 REST backend** with MySQL.
- An **HTML5/CSS3/Vanilla JavaScript** frontend.
- An **AI Research Agent** orchestrating **4 controlled Java tools** with document access clearance checks enforced *before* any text is read or supplied to an AI model.

---

## 2. Key Features

- **Multi-Format Document Ingestion:** Upload plain-text (`.txt`) and PDF (`.pdf`) documents (processed via Apache PDFBox).
- **Automated Text Chunking & Indexing:** Divides documents into granular passages with section titles, version IDs, and estimated page numbers.
- **Hierarchical Access Clearance:** Role-based visibility (`PUBLIC`, `EMPLOYEE`, `HR`, `ADMIN`) evaluated in Java.
- **Transparent Keyword Search & Scoring:** Fast keyword scoring based on phrase matches, term frequencies, and title/section headers.
- **Goal-Driven AI Research Agent:**
  - Retrieves only authorized evidence.
  - Validates factual overlap and **explicitly abstains** if evidence is insufficient.
  - Synthesizes multi-document answers.
  - Returns verifiable source citations.
- **Dual Execution Modes:** Runs in deterministic **SIMULATION** mode with zero external dependencies, or connects to **REAL_AI** (OpenAI / Spring AI compatible) when configured.
- **Prompt-Injection Neutralizer:** Neutralizes malicious instructions embedded within uploaded documents (e.g. *"Ignore previous instructions"*).
- **Document Versioning & Re-Indexing:** Monotonically increments document version and excludes obsolete chunks.
- **Answer Quality Feedback:** 1 to 5 star rating system persisted in MySQL.
- **Administrative Analytics:** Dashboard metrics for asset composition, re-index queues, search demand, and role visibility simulation.

---

## 3. Technology Stack

- **Backend:** Java 21 LTS, Spring Boot 3.3.4, Spring Web (REST), Spring Data JPA, Hibernate, Java Bean Validation.
- **Database:** MySQL Server 8.0+ / 26.x.
- **Security:** Spring Security with BCrypt password hashing and role-based clearance.
- **File Processing:** Apache PDFBox 2.0.31 (PDF extraction) and Java NIO UTF-8 text parser.
- **Frontend:** Semantic HTML5, Vanilla CSS3 (curated design, no frameworks), Vanilla JavaScript (`fetch()` API).
- **Testing:** JUnit 5, Mockito, AssertJ, Spring Boot Test, H2 In-Memory Database (for isolated fast test runs).
- **Build Tool:** Apache Maven 3.9.6.
- **Version Control:** Git.

---

## 4. Architecture and Request Flow

The application follows a simple, robust layered architecture:

```
[Browser / Frontend Client]
        │
        ▼ (HTTP REST / JSON / Multipart Form)
[Controller Layer]
  - AuthController, KnowledgeDocumentController, SearchController,
    AiAgentController, FeedbackController, AnalyticsController
        │
        ▼ (Java DTOs & Validation)
[Service Layer]
  - KnowledgeDocumentService, DocumentParserService, TextChunkerService,
    SearchService, PermissionService, FeedbackService, AnalyticsService
  - AiResearchAgent & KnowledgeAgentTools (4 Controlled Tools)
        │
        ▼ (Spring Data JPA)
[Repository Layer]
  - JpaRepositories (Document, Chunk, Feedback, Search, User)
        │
        ▼ (JDBC / MySQL Connector)
[MySQL Database] & [Local Disk Storage 'uploads/']
```

### Explaining the Core Annotations (For Freshers)
- `@RestController`: Marks the class as a web controller where methods return JSON responses directly.
- `@Service`: Marks a class holding business logic and operations.
- `@Repository`: Marks an interface managing database communication through Spring Data JPA.
- `@Entity` & `@Table`: Maps a Java class to a MySQL database table.
- `@Id` & `@GeneratedValue`: Specifies the auto-increment primary key.
- `@Transactional`: Guarantees that multi-step database writes (like creating a document and saving all its chunks) execute as an atomic transaction. If an error occurs, all changes roll back to prevent partial indexing.

---

## 5. Database Schema and Relationships

The database schema is defined in `database/schema.sql`:

1. **`users`**: Stores user credentials, hashed passwords (`BCrypt`), and roles (`ROLE_EMPLOYEE`, `ROLE_HR`, `ROLE_ADMIN`).
2. **`knowledge_documents`**: Stores document metadata (`title`, `category`, `access_level`, `version`, `status`, `original_file_name`, `searchable_text`, `uploaded_at`, `needs_reindex`).
3. **`knowledge_chunks`**: Stores segmented text blocks linked to `document_id` and `document_version`.
4. **`knowledge_feedback`**: Stores user ratings (1 to 5), questions, and answer references linked to `document_id`.
5. **`search_history`**: Logs search queries and category filters for analytics.

```
+--------------------+            +------------------------+
|       users        |            |  knowledge_documents   |
+--------------------+            +------------------------+
| id (PK)            |            | id (PK)                |
| username           |            | title                  |
| password (BCrypt)  |            | category               |
| role               |            | access_level           |
| enabled            |            | version                |
+--------------------+            | status                 |
                                  +------------------------+
                                         │ 1
                                         │
                                         ├──────────────┐
                                         │              │
                                         ▼ N            ▼ N
                               +------------------+   +--------------------+
                               | knowledge_chunks |   | knowledge_feedback |
                               +------------------+   +--------------------+
                               | id (PK)          |   | id (PK)            |
                               | document_id (FK) |   | document_id (FK)   |
                               | document_version |   | rating (1..5)      |
                               | chunk_index      |   | question           |
                               | section_reference|   | answer_reference   |
                               | content          |   | user_id            |
                               +------------------+   +--------------------+
```

---

## 6. Prerequisites

Ensure you have the following installed on your machine:
- **Java 17 or Java 21** (`java -version`)
- **Apache Maven 3.8+ or 3.9+** (`mvn -version`)
- **MySQL Server 8.0+ or 26.x** running locally on port 3306
- **Git** (`git --version`)
- A modern web browser (Chrome, Edge, Firefox)

---

## 7. Java and Maven Setup

Verify Java and Maven are available on your system path by opening a terminal (PowerShell or CMD) and running:
```powershell
java -version
mvn -version
```
Expected output:
```
java version "21.0.12.1"
Apache Maven 3.9.6
```

---

## 8. MySQL Server Setup

> **Important Concept for Freshers:**
> - **MySQL Server** is the actual database service running in the background (as a Windows Service, typically named `MySQL80` or `MySQL267`) listening on TCP port 3306.
> - **MySQL Workbench** is merely a visual desktop user interface (client application) used to query and manage the database server. Installing Workbench alone does not run a database server!

### Checking MySQL Server on Windows:
To verify that your MySQL Server service is running in PowerShell:
```powershell
Get-Service -Name *mysql*
```
If the status is `Stopped`, start it with:
```powershell
Start-Service -Name MySQL267   # Replace with your MySQL service name
```

---

## 9. Database Creation and Migrations

Log in to MySQL using the CLI or Workbench and run the schema setup:
```powershell
mysql -u root -p
```
Run the following SQL commands (or execute `database/schema.sql` and `database/seed-data.sql`):
```sql
CREATE DATABASE IF NOT EXISTS knowledge_portal_db;
USE knowledge_portal_db;
```
You can import the schema and seed data directly:
```powershell
mysql -u root -proot knowledge_portal_db < database/schema.sql
mysql -u root -proot knowledge_portal_db < database/seed-data.sql
```
*Note: Even if you do not run seed scripts manually, Spring Boot's built-in `DataInitializer` component will automatically seed the initial users and approved policies upon first application startup!*

---

## 10. Environment Configuration

Copy the sample environment file `.env.example` to `.env` or set environment variables:
```powershell
cp .env.example .env
```
Default parameters in `src/main/resources/application.properties`:
```properties
server.port=8080
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/knowledge_portal_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
knowledge.upload.dir=${UPLOAD_DIR:./uploads}

# Optional AI Configuration:
knowledge.ai.api-key=${AI_API_KEY:}
knowledge.ai.base-url=${AI_API_BASE_URL:https://api.openai.com/v1}
knowledge.ai.model=${AI_MODEL_NAME:gpt-4o-mini}
```

---

## 11. How to Run the Backend

To compile and launch the Spring Boot application, run:
```powershell
mvn spring-boot:run
```
Or package a standalone JAR and run it:
```powershell
mvn clean package -DskipTests
java -jar target/knowledge-portal-1.0.0-SNAPSHOT.jar
```
When started, Spring Boot will log:
```
Started KnowledgePortalApplication in ... seconds (process running ...)
```

---

## 12. How to Access the Frontend

Once the backend is running, open your web browser and navigate to:
```
http://localhost:8080/
```

### Pre-Configured Test Personas:
The top header includes a convenient **Persona Switcher** allowing you to switch between user roles instantly for demonstrations:

| Username | Password | Role | Clearance Levels |
| :--- | :--- | :--- | :--- |
| `employee1` | `password123` | **ROLE_EMPLOYEE** | `PUBLIC`, `EMPLOYEE` |
| `hr_user` | `password123` | **ROLE_HR** | `PUBLIC`, `EMPLOYEE`, `HR` |
| `admin_user` | `password123` | **ROLE_ADMIN** | `PUBLIC`, `EMPLOYEE`, `HR`, `ADMIN` |

---

## 13. How to Upload Sample Documents

1. Click on the **Document Management** tab in the navigation bar.
2. Fill out the upload form:
   - **Document Title:** e.g., `Hybrid Office Attendance Policy`
   - **Category:** Select `POLICY`, `TECHNICAL_RUNBOOK`, or `FAQ`.
   - **Clearance Level:** Select `PUBLIC`, `EMPLOYEE`, `HR`, or `ADMIN`.
   - **File:** Choose a `.txt` or `.pdf` file (max 10MB).
3. Click **Upload and Index Document**.
4. The system will extract text, segment it into chunks, persist the document transactionally, and immediately display it in the table.

---

## 14. How to Test Search and AI Question Answering

### Testing Keyword Search:
1. Navigate to the **Knowledge Search** tab.
2. Enter keywords such as `annual leave` or `severity 1 war room`.
3. Select optional category and status filters.
4. Click **Search**. The system retrieves matching passages with computed relevance scores and highlighted snippets.
5. Click **View Full Passage** to inspect the source text in a modal.

### Testing AI Question Answering:
1. Navigate to the **AI Research Agent** tab.
2. Click any of the pre-set quick queries or type your own question:
   - *Example 1 (Factual support):* `"How many days of annual leave are available?"`
     - **Result:** Factual answer (25 days) with citation to Document #1, Section 1.
   - *Example 2 (Multi-document synthesis):* `"What is the annual leave carry over rule and what is the home office allowance?"`
     - **Result:** Synthesized answer citing both Document #1 and Document #5.
   - *Example 3 (Abstention):* `"What is the recipe for chocolate chip cookies?"`
     - **Result:** Explicit abstention: *"I cannot find sufficient evidence in the approved enterprise documents accessible to your clearance level to answer this question reliably."*
   - *Example 4 (Security clearance check):* As `employee1`, ask: `"What are the executive bonuses and EBITDA targets?"`
     - **Result:** Agent abstains because Document #3 is restricted to `HR` and `ADMIN`. Switch to `hr_user` in the header, ask again, and observe the answer unlocked!

---

## 15. How to Configure Real AI Access

The application runs in deterministic **SIMULATION** mode by default with zero external dependencies.

To enable **REAL_AI** mode using an OpenAI-compatible model (e.g., OpenAI `gpt-4o-mini`, Azure OpenAI, or local Ollama):
1. In your `.env` file (or operating system environment variables), set:
   ```properties
   AI_API_KEY=sk-your-actual-api-key-here
   AI_API_BASE_URL=https://api.openai.com/v1
   AI_MODEL_NAME=gpt-4o-mini
   ```
2. Restart the application.
3. The UI will now display the badge `REAL_AI` on generated answers.
4. If an external API call times out or fails, the agent automatically falls back to `SIMULATION_FALLBACK` without interrupting service.

---

## 16. How to Run Automated Unit and Integration Tests

Run the full automated test suite using Maven:
```powershell
mvn clean test
```
### Test Coverage:
All 15 automated test cases execute against an isolated in-memory H2 database profile (`application-test.properties`):
- `TC-05-01`: Upload supported non-empty document -> indexed & searchable.
- `TC-05-01-PDF`: PDF text extraction & indexing with Apache PDFBox.
- `TC-05-02`: Rejection of empty files and unsupported extensions (`.exe`).
- `TC-05-03`: Factual answer generation & citation validation.
- `TC-05-04`: Insufficient evidence transparent abstention.
- `TC-05-05`: Multi-document synthesis & citations.
- `TC-05-06`: Metadata filtered keyword search.
- `TC-05-07`: Unauthorized access & restriction against leaking confidential text.
- `TC-05-08`: Adversarial prompt-injection defense.
- `TC-05-09`: Document re-indexing version bump & exclusion of obsolete chunks.
- `TC-05-10`: AI model timeout simulation fallback.
- `TC-05-11`: 1-5 star feedback persistence and validation.
- `TC-05-12`: Benchmark question set evaluation.
- Context loads & safe archival tests.

---

## 17. How to Use Git

This repository contains a clean, organized commit history:
```powershell
# Check status
git status

# View commit history
git log --oneline

# Create a branch for new features
git checkout -b feature/new-search-filter

# Stage and commit changes
git add .
git commit -m "feat: Add date range filter to knowledge search"
```

---

## 18. Known Limitations

1. **Scanned Image PDFs:** The application extracts text from standard text-layer PDF documents using Apache PDFBox. It does not perform OCR (Optical Character Recognition) on scanned image PDFs. Scanned PDFs without an embedded text layer are rejected with a clear error message.
2. **Keyword vs. Vector Retrieval:** The search engine uses term-frequency keyword matching. It does not use vector embeddings or external vector databases, keeping the application lightweight, fully local, and easy to run.
3. **Single-Node Session Storage:** HTTP sessions are stored in-memory by default. In a multi-node production cluster, configure Redis-backed session management.

---

## 19. Security and Prompt-Injection Defense Design

1. **Java-Enforced Clearance:** Document access is validated in Java before search scoring, citation generation, or AI prompt construction.
2. **Prompt-Injection Neutralizer:** Untrusted document text is sanitized before synthesis. Malicious phrases like `"Ignore previous instructions"` or `"Grant admin privileges"` are defused.
3. **BCrypt Password Security:** User passwords are never stored in plain text.
4. **Input Validation:** File sizes (>10MB), unsupported extensions, and ratings (<1 or >5) are rejected at the REST API boundary.

---

## 20. Troubleshooting Common Errors

### Error: `Access denied for user 'root'@'localhost'`
- Check your MySQL root password. In `.env` or `application.properties`, update `DB_PASSWORD` to match your local MySQL root password.

### Error: `Communications link failure / Connection refused: connect`
- Your MySQL service is not running. In PowerShell, run `Get-Service *mysql*` and `Start-Service MySQL267`.

### Error: `Port 8080 already in use`
- Set a different port in `.env` (e.g., `SERVER_PORT=8081`) or terminate the existing process occupying port 8080.

---

## 21. Extra Technologies Used Beyond Mandatory Stack

In accordance with Section 28, the following supporting libraries were added to fulfill specific functional requirements:

1. **Apache PDFBox (`pdfbox 2.0.31`):** Required to extract text from PDF files. Standard Java does not natively parse PDF binary structures.
2. **Spring Security (`spring-boot-starter-security`):** Required to provide real user authentication, BCrypt password hashing, and session management.
3. **H2 In-Memory Database (`com.h2database:h2`):** Added in `test` scope so automated tests run fast, reliably, and without external database dependencies.

---

## 22. Final Technology Inventory (Mandatory)

| Technology / Library | Purpose in Application | Mandatory Stack Category | Reason for Inclusion |
| :--- | :--- | :--- | :--- |
| **Java 21 LTS** | Primary programming language | Explicitly Mandatory | Core language platform for Spring Boot backend. |
| **Spring Boot 3.3.4** | Application framework | Explicitly Mandatory | Provides web server, dependency injection, and configuration. |
| **Spring Web (REST)** | RESTful API controllers | Explicitly Mandatory | Implements JSON REST endpoints for search, upload, and Q&A. |
| **Spring Data JPA** | Database persistence & ORM | Explicitly Mandatory | Bridges Java entities to relational database tables with Hibernate. |
| **MySQL Server (26.7 / 8.0+)** | Relational database | Explicitly Mandatory | Persists documents, chunks, users, feedback, and search history. |
| **HTML5** | Web page structure | Explicitly Mandatory | Clean semantic UI structure for all 7 portal screens. |
| **CSS3** | Visual styling & design system | Explicitly Mandatory | Modern responsive layout, typography, cards, badges, and modals. |
| **Vanilla JavaScript** | Client-side scripting | Explicitly Mandatory | Handles persona switching, API calls via `fetch()`, and UI updates. |
| **JUnit 5 (Jupiter)** | Automated unit & integration testing | Explicitly Mandatory | Executes test cases TC-05-01 through TC-05-12. |
| **Mockito** | Mocking framework | Explicitly Mandatory | Mocks AI client during timeout simulation tests. |
| **Git** | Version control system | Explicitly Mandatory | Tracks milestone commits across development phases. |
| **Maven 3.9.6** | Dependency & build management | Necessary Supporting Tool | Standard build tool for Java and Spring Boot. |
| **Apache PDFBox 2.0.31** | PDF text extraction | Necessary Supporting Tool | Extracts readable text from uploaded `.pdf` documents. |
| **Spring Security** | Authentication & clearance | Necessary Supporting Tool | Role-based authorization (`EMPLOYEE`, `HR`, `ADMIN`) & BCrypt. |
| **H2 Database (Test Scope)** | Fast isolated test database | Necessary Supporting Tool | Allows `mvn test` to run zero-configuration tests in memory. |
| **OpenAI Compatible Client** | Optional live AI model provider | Optional Service / Integration | Connects to `gpt-4o-mini` when `AI_API_KEY` is provided. |
