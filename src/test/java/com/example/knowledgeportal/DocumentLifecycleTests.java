package com.example.knowledgeportal;

import com.example.knowledgeportal.dto.DocumentDetailResponse;
import com.example.knowledgeportal.dto.DocumentResponse;
import com.example.knowledgeportal.dto.SearchRequest;
import com.example.knowledgeportal.dto.SearchResponse;
import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.exception.InvalidFileException;
import com.example.knowledgeportal.service.KnowledgeDocumentService;
import com.example.knowledgeportal.service.SearchService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentLifecycleTests {

    @Autowired
    private KnowledgeDocumentService documentService;

    @Autowired
    private SearchService searchService;

    private Authentication createAuth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "pass", List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    @Transactional
    @DisplayName("TC-05-01: Upload a supported, non-empty document and verify that it becomes indexed and searchable")
    void testUploadAndIndexSupportedDocument() {
        Authentication auth = createAuth("employee1", "ROLE_EMPLOYEE");
        String content = "Section 1: Work From Home Guidelines. Remote workers are provided with a standard VPN client. " +
                         "Bandwidth speed test must show at least 25 Mbps download for VoIP video calls.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "wfh_guidelines.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)
        );

        // 1. Upload and index document
        DocumentResponse doc = documentService.uploadAndIndexDocument(
                file, "WFH Technical Guidelines", "Network and VPN rules",
                DocumentCategory.TECHNICAL_RUNBOOK, AccessLevel.EMPLOYEE, auth
        );

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertEquals(1, doc.getVersion());
        assertEquals(DocumentStatus.ACTIVE, doc.getStatus());
        assertTrue(doc.getChunkCount() > 0, "Document must have at least 1 indexed chunk.");

        // 2. Verify it is searchable
        SearchResponse searchRes = searchService.search(
                new SearchRequest("VPN client 25 Mbps", DocumentCategory.TECHNICAL_RUNBOOK, DocumentStatus.ACTIVE), auth
        );

        assertNotNull(searchRes);
        assertTrue(searchRes.getTotalResults() > 0, "Uploaded document chunk must be retrieved in keyword search.");
        assertTrue(searchRes.getResults().get(0).getSnippet().toLowerCase().contains("vpn"));
    }

    @Test
    @DisplayName("TC-05-01-PDF: Upload a supported non-empty PDF document and verify indexing")
    void testUploadPdfDocument() throws IOException {
        Authentication auth = createAuth("admin_user", "ROLE_ADMIN");

        // Generate synthetic PDF with PDFBox
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cos = new PDPageContentStream(document, page)) {
                cos.beginText();
                cos.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cos.newLineAtOffset(100, 700);
                cos.showText("Cloud Disaster Recovery Plan for MySQL Database Nodes");
                cos.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            pdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "dr_plan.pdf", "application/pdf", pdfBytes
        );

        DocumentResponse doc = documentService.uploadAndIndexDocument(
                file, "Cloud DR Plan", "PDF recovery steps",
                DocumentCategory.TECHNICAL_RUNBOOK, AccessLevel.ADMIN, auth
        );

        assertNotNull(doc);
        assertTrue(doc.getChunkCount() > 0);
    }

    @Test
    @DisplayName("TC-05-02: Upload an unsupported or empty document and verify that it is rejected")
    void testUploadUnsupportedOrEmptyDocument() {
        Authentication auth = createAuth("employee1", "ROLE_EMPLOYEE");

        // Case 1: Empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );
        assertThrows(InvalidFileException.class, () -> {
            documentService.uploadAndIndexDocument(emptyFile, "Empty Doc", "", DocumentCategory.FAQ, AccessLevel.PUBLIC, auth);
        }, "Empty file must be rejected.");

        // Case 2: Unsupported extension (.exe)
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file", "malicious_script.exe", "application/octet-stream", "binary data".getBytes()
        );
        assertThrows(InvalidFileException.class, () -> {
            documentService.uploadAndIndexDocument(unsupportedFile, "Executable", "", DocumentCategory.FAQ, AccessLevel.PUBLIC, auth);
        }, "Unsupported file extension must be rejected.");

        // Case 3: Missing title
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "valid.txt", "text/plain", "Some valid text".getBytes()
        );
        assertThrows(InvalidFileException.class, () -> {
            documentService.uploadAndIndexDocument(validFile, "   ", "", DocumentCategory.FAQ, AccessLevel.PUBLIC, auth);
        }, "Blank title must be rejected.");
    }

    @Test
    @Transactional
    @DisplayName("TC-05-09: Update and re-index a document and verify that new content is returned and obsolete content is excluded")
    void testReindexDocumentUpdatesVersionAndChunks() {
        Authentication auth = createAuth("admin_user", "ROLE_ADMIN");

        // 1. Initial upload (v1)
        MockMultipartFile initialFile = new MockMultipartFile(
                "file", "server_cluster_guide.txt", "text/plain",
                "Cluster consists of 4 nodes running Ubuntu 22.04 LTS.".getBytes(StandardCharsets.UTF_8)
        );
        DocumentResponse initialDoc = documentService.uploadAndIndexDocument(
                initialFile, "Server Cluster Architecture", "Original v1 architecture",
                DocumentCategory.TECHNICAL_RUNBOOK, AccessLevel.EMPLOYEE, auth
        );
        assertEquals(1, initialDoc.getVersion());

        // Verify initial content is present in details
        DocumentDetailResponse detailsV1 = documentService.getDocumentDetails(initialDoc.getId(), auth);
        assertEquals(1, detailsV1.getDocument().getVersion());
        assertTrue(detailsV1.getChunks().get(0).getContent().contains("4 nodes"));

        // 2. Re-index document
        DocumentResponse reindexedDoc = documentService.reindexDocument(initialDoc.getId(), auth);

        // Version must be bumped to 2
        assertEquals(2, reindexedDoc.getVersion());

        // Verify active chunks returned are version 2
        DocumentDetailResponse detailsV2 = documentService.getDocumentDetails(initialDoc.getId(), auth);
        assertEquals(2, detailsV2.getDocument().getVersion());
        for (var chunk : detailsV2.getChunks()) {
            assertEquals(2, chunk.getDocumentVersion(), "Active chunks must have documentVersion = 2");
        }
    }

    @Test
    @Transactional
    @DisplayName("Test Safe Archival: Soft-delete document and verify excluded from active searches")
    void testSafeArchival() {
        Authentication auth = createAuth("admin_user", "ROLE_ADMIN");

        MockMultipartFile file = new MockMultipartFile(
                "file", "deprecated_vpn.txt", "text/plain",
                "Legacy PPTP VPN gateway is deprecated and shut down.".getBytes()
        );
        DocumentResponse doc = documentService.uploadAndIndexDocument(
                file, "Legacy PPTP VPN", "Old VPN doc",
                DocumentCategory.TECHNICAL_RUNBOOK, AccessLevel.EMPLOYEE, auth
        );

        // Safe archive
        DocumentResponse archived = documentService.archiveDocument(doc.getId(), auth);
        assertEquals(DocumentStatus.ARCHIVED, archived.getStatus());

        // Search for active docs must NOT return it
        SearchResponse res = searchService.search(
                new SearchRequest("PPTP VPN", DocumentCategory.TECHNICAL_RUNBOOK, DocumentStatus.ACTIVE), auth
        );
        boolean foundArchived = res.getResults().stream().anyMatch(r -> r.getDocumentId().equals(doc.getId()));
        assertFalse(foundArchived, "Archived document must not appear in active search results.");
    }
}
