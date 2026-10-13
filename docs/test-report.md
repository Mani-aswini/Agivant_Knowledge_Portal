# Automated Test Report
## Enterprise Knowledge Portal with AI Research Agent

### 1. Test Execution Summary

- **Framework:** JUnit 5 (Jupiter), Spring Boot Test (`@SpringBootTest`), Mockito
- **Execution Date:** 2026-10-13
- **Total Tests Executed:** 15
- **Passed:** 15
- **Failed:** 0
- **Errors:** 0
- **Skipped:** 0
- **Pass Rate:** 100%
- **Build Status:** SUCCESS

---

### 2. Test Case Traceability Matrix

| Test ID | Test Method & Class | Requirement / Objective | Result |
| :--- | :--- | :--- | :--- |
| **TC-05-01** | `DocumentLifecycleTests.testUploadAndIndexSupportedDocument` | Upload supported non-empty document (`.txt`) and verify indexed and searchable. | **PASSED** |
| **TC-05-01-PDF** | `DocumentLifecycleTests.testUploadPdfDocument` | Upload supported non-empty PDF document (Apache PDFBox) and verify indexed chunks. | **PASSED** |
| **TC-05-02** | `DocumentLifecycleTests.testUploadUnsupportedOrEmptyDocument` | Upload empty document, unsupported extension (`.exe`), and blank title; verify rejection. | **PASSED** |
| **TC-05-03** | `AiResearchAgentTests.testQuestionAnsweredBySourceDocument` | Ask question answered by source doc; verify factual answer (25 days) and citations. | **PASSED** |
| **TC-05-04** | `AiResearchAgentTests.testQuestionWithNoEvidenceAbstains` | Ask question with no evidence (e.g. lasagna recipe); verify transparent abstention. | **PASSED** |
| **TC-05-05** | `AiResearchAgentTests.testQuestionRequiringTwoDocumentsCitesBoth` | Ask question requiring two documents; verify both sources (Doc 1 & Doc 5) are cited. | **PASSED** |
| **TC-05-06** | `KnowledgeSearchAndSecurityTests.testSearchWithMetadataFilters` | Search with category filters (`POLICY`, `TECHNICAL_RUNBOOK`); verify filtered matches. | **PASSED** |
| **TC-05-07** | `KnowledgeSearchAndSecurityTests.testRestrictedContentNotLeakedToUnauthorizedUser` | Query restricted HR/ADMIN docs as unauthorized employee; verify zero leaks. | **PASSED** |
| **TC-05-08** | `AiResearchAgentTests.testPromptInjectionDefense` | Include prompt injection in document; verify permissions and rules cannot be overridden. | **PASSED** |
| **TC-05-09** | `DocumentLifecycleTests.testReindexDocumentUpdatesVersionAndChunks` | Re-index document; verify version bumped to 2 and obsolete v1 chunks excluded. | **PASSED** |
| **TC-05-10** | `AiResearchAgentTests.testModelTimeoutTriggersSafeSimulationFallback` | Simulate AI model timeout; verify safe fallback to extractive simulation (`SIMULATION_FALLBACK`). | **PASSED** |
| **TC-05-11** | `KnowledgeSearchAndSecurityTests.testSubmitFeedbackPersistenceAndValidation` | Submit 5-star feedback; verify MySQL persistence; test invalid ratings (<1 or >5). | **PASSED** |
| **TC-05-12** | `AiResearchAgentTests.testFixedQuestionEvaluationSet` | Evaluate benchmark question set against factual criteria and citation validity. | **PASSED** |
| **ARCHIVE** | `DocumentLifecycleTests.testSafeArchival` | Soft-delete document; verify marked ARCHIVED and excluded from search. | **PASSED** |
| **CONTEXT** | `KnowledgePortalApplicationTests.contextLoads` | Verify Spring Boot context, dependency injection, and JPA beans bootstrap cleanly. | **PASSED** |

---

### 3. How to Run Tests Locally

Execute the standard Maven test command from the repository root:
```powershell
mvn clean test
```
All tests run against an isolated in-memory test database profile (`application-test.properties`), requiring zero manual external database or cloud credentials.
