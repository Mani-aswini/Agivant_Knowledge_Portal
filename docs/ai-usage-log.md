# AI Usage and Development Log

## 1. Development Prompts & Strategic Instructions

The application was built following a phased engineering approach designed to maximize code readability and architectural cleanliness for a beginner/fresher:
- **Phase 1:** Foundation setup (`pom.xml`, `.gitignore`, `.env.example`, database scripts for MySQL and H2, `KnowledgePortalApplication`).
- **Phase 2:** Persistence layer, JPA entities (`KnowledgeDocument`, `KnowledgeChunk`, `KnowledgeFeedback`, `SearchHistory`, `UserAccount`), repositories, and Spring Security authentication.
- **Phase 3:** Document ingestion, Apache PDFBox text extraction, paragraph/window chunking, document versioning, and lifecycle endpoints.
- **Phase 4:** Multi-term keyword search with transparent scoring, feedback API, and SQL analytics.
- **Phase 5:** Controlled agentic AI tool workflow, prompt-injection defense, and dual REAL_AI / SIMULATION execution modes.
- **Phase 6:** Responsive frontend in vanilla HTML5, CSS3, and JavaScript with 7 screens and persona switcher.
- **Phase 7:** Automated testing implementing test cases TC-05-01 through TC-05-12.
- **Phase 8:** Complete documentation and technology inventory.

---

## 2. Tools Called by the Agent and Their Purposes

| Tool | Purpose |
| :--- | :--- |
| `list_dir` | Inspected initial workspace directory structure to confirm empty state. |
| `run_command` | Inspected Java (21.0.12.1), Maven (3.9.6), Git (2.55.0), and verified running MySQL Server 26.7 service (`MySQL267` on port 3306). |
| `write_to_file` | Generated source code, configurations, database scripts, test classes, and documentation files. |
| `replace_file_content` | Refined and debugged code (fixing class reference in main class and updating test TC-05-07). |
| `mvn test` | Executed JUnit 5 automated test suite to verify test cases and identify defects. |
| `git commit` | Created atomic, meaningful Git commits for completed milestones. |

---

## 3. Actual Defect Discovered During Development

### Defect Description
During execution of the test suite (`mvn clean test`), test case **`TC-05-07`** failed:
```
[ERROR] com.example.knowledgeportal.KnowledgeSearchAndSecurityTests.testRestrictedContentNotLeakedToUnauthorizedUser <<< FAILURE!
org.opentest4j.AssertionFailedError: ROLE_EMPLOYEE must not receive chunks from HR restricted documents. ==> expected: <0> but was: <3>
	at com.example.knowledgeportal.KnowledgeSearchAndSecurityTests.testRestrictedContentNotLeakedToUnauthorizedUser(KnowledgeSearchAndSecurityTests.java:78)
```

### Reproduction Steps
1. Seed the database with Document 1 (Annual Leave, clearance `EMPLOYEE`) and Document 3 (Executive Bonus, clearance `HR`).
2. Run search as `employee1` with query: `"Executive annual performance bonuses EBITDA"`.
3. The test asserted `assertEquals(0, hrSearch.getTotalResults())`.
4. The test failed because 3 results were returned instead of 0.

### Root Cause Analysis
Seed Document 1 ("Company Annual Leave and Time Off Policy") is accessible to `ROLE_EMPLOYEE` and contains the word `"annual"` across each of its 3 chunks ("paid annual leave", "unused annual leave", "paid days annually").
Because the search query contained the common word `"annual"`, the search service matched Document 1 (which `employee1` is authorized to see) and returned its chunks.
Restricted Document 3 was **correctly withheld** by `PermissionService`, but the test assertion incorrectly expected zero total results for a query containing words present in authorized documents.

### How It Was Fixed
In `KnowledgeSearchAndSecurityTests.java`:
1. Updated the query for testing HR-specific keywords to `"EBITDA corporate benchmarks bonus tiers"` (terms exclusive to Document 3). This cleanly verified that 0 results were returned to an employee.
2. Added an assertion for mixed queries (containing both common terms and restricted terms):
   ```java
   SearchResponse broadSearch = searchService.search(
       new SearchRequest("annual performance policy bonus bastion", null, DocumentStatus.ACTIVE), employeeAuth
   );
   boolean containsRestrictedDoc = broadSearch.getResults().stream()
       .anyMatch(r -> r.getDocumentId().equals(3L) || r.getDocumentId().equals(4L));
   assertFalse(containsRestrictedDoc, "Search results must never contain Document 3 (HR) or Document 4 (ADMIN) for an employee.");
   ```
3. Added direct detail retrieval assertions verifying that calling `getDocumentDetails()` on Document 3 or 4 throws `UnauthorizedAccessException`.

### Verification of the Fix
Re-ran `mvn test`:
```
[INFO] Running com.example.knowledgeportal.KnowledgeSearchAndSecurityTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.knowledgeportal.KnowledgeSearchAndSecurityTests
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 4. Method Used to Check Factual Support and Citation Correctness

To verify factual support and citation correctness (e.g. in test TC-05-12):
1. **Factual Support:** Rather than matching exact sentence strings (which can vary depending on capitalization or synthesis formatting), tests evaluate whether key numerical values and factual tokens (e.g., `"25"`, `"10"`, `"$500"`) appear in the answer text.
2. **Citation Correctness:** The response `sources` list is checked to ensure that:
   - The cited `documentId` exactly matches the expected source document.
   - The cited document title corresponds to the approved document.
   - The `chunkIndex` and section reference point to the verified chunk.
   - On abstention (when evidence is lacking), `sources` is verified to be strictly empty.

---

## 5. Execution Mode Status

- The implementation includes full support for both **REAL_AI** and **SIMULATION** modes.
- By default (when `AI_API_KEY` is not provided), the system runs in deterministic **SIMULATION** mode, producing factual answers with verified citations without external network costs or risks.
- If `AI_API_KEY` is configured in `.env` (or environment variables), the system communicates with the configured OpenAI-compatible endpoint with automatic fallback to `SIMULATION_FALLBACK` upon timeout.

---

## 6. Current Limitations and Production Integration Steps

1. **Scanned PDF OCR:** The current implementation extracts text from vector/text-layer PDFs using Apache PDFBox. Non-text scanned image PDFs require Tesseract OCR or cloud document intelligence.
2. **Clustered Caching:** For multi-node production deployment, session management can be transitioned to Spring Session with Redis.
3. **Database Security:** In production, use dedicated least-privilege MySQL user credentials rather than root.
