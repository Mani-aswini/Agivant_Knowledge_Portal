package com.example.knowledgeportal.service;

import com.example.knowledgeportal.dto.ChunkResponse;
import com.example.knowledgeportal.dto.DocumentDetailResponse;
import com.example.knowledgeportal.dto.DocumentResponse;
import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentCategory;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.entity.KnowledgeChunk;
import com.example.knowledgeportal.entity.KnowledgeDocument;
import com.example.knowledgeportal.exception.InvalidFileException;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.repository.KnowledgeChunkRepository;
import com.example.knowledgeportal.repository.KnowledgeDocumentRepository;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing document ingestion, chunking, versioning, and lifecycle.
 */
@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final DocumentParserService documentParserService;
    private final TextChunkerService textChunkerService;
    private final PermissionService permissionService;
    private final com.example.knowledgeportal.repository.DocumentVersionRepository documentVersionRepository;
    private final AuditLogService auditLogService;
    private final ProcessingJobService processingJobService;

    @Value("${knowledge.upload.dir:./uploads}")
    private String uploadDirectory;

    public KnowledgeDocumentService(KnowledgeDocumentRepository documentRepository,
                                  KnowledgeChunkRepository chunkRepository,
                                  DocumentParserService documentParserService,
                                  TextChunkerService textChunkerService,
                                  PermissionService permissionService,
                                  com.example.knowledgeportal.repository.DocumentVersionRepository documentVersionRepository,
                                  AuditLogService auditLogService,
                                  ProcessingJobService processingJobService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentParserService = documentParserService;
        this.textChunkerService = textChunkerService;
        this.permissionService = permissionService;
        this.documentVersionRepository = documentVersionRepository;
        this.auditLogService = auditLogService;
        this.processingJobService = processingJobService;
    }

    /**
     * Uploads and indexes a new document inside a database transaction.
     * Guaranteed not to leave partially indexed records if processing fails.
     */
    @Transactional
    public DocumentResponse uploadAndIndexDocument(MultipartFile file, String title, String description,
                                                  DocumentCategory category, AccessLevel accessLevel,
                                                  Authentication auth) {
        return uploadAndIndexDocument(file, title, description, category, null, accessLevel, auth);
    }

    @Transactional
    public DocumentResponse uploadAndIndexDocument(MultipartFile file, String title, String description,
                                                  DocumentCategory category, String customCategory,
                                                  AccessLevel accessLevel,
                                                  Authentication auth) {
        if (title == null || title.trim().isBlank()) {
            throw new InvalidFileException("Document title cannot be empty.");
        }
        if (category == null) {
            throw new InvalidFileException("Document category is required.");
        }
        if (category == DocumentCategory.OTHER) {
            if (customCategory == null || customCategory.trim().isBlank()) {
                throw new InvalidFileException("Please enter a category name when selecting 'Other'.");
            }
        }
        if (accessLevel == null) {
            throw new InvalidFileException("Document access clearance level is required.");
        }

        String owner = permissionService.getCurrentUsername(auth);

        // 1. Extract text and validate file type and usable content
        DocumentParserService.ExtractedDocument parsed = documentParserService.parseUploadedFile(file);

        // 2. Safely store the original file on disk outside public static directories
        String storedFilePath = storeFileToDisk(file);

        // 3. Create and persist document entity
        KnowledgeDocument document = new KnowledgeDocument(
                title.trim(),
                (description != null) ? description.trim() : "",
                category,
                owner,
                accessLevel,
                parsed.getOriginalFilename(),
                parsed.getContentType(),
                parsed.getText(),
                storedFilePath
        );
        if (category == DocumentCategory.OTHER && customCategory != null && !customCategory.trim().isBlank()) {
            document.setCustomCategory(customCategory.trim());
        }
        document = documentRepository.save(document);

        com.example.knowledgeportal.entity.ProcessingJob job = null;
        try {
            job = processingJobService.recordJobStart(document.getId(), document.getTitle(), document.getVersion(), com.example.knowledgeportal.entity.JobType.INGESTION);
        } catch (Exception ignored) {}

        // 4. Segment text into granular chunks
        List<TextChunkerService.ChunkDraft> chunkDrafts;
        try {
            chunkDrafts = textChunkerService.splitIntoChunks(parsed.getText());
            if (chunkDrafts.isEmpty()) {
                throw new InvalidFileException("Document content could not be segmented into searchable chunks.");
            }
        } catch (Exception e) {
            if (job != null) processingJobService.recordJobFailure(job.getId(), e.getMessage());
            document.setIndexingStatus(com.example.knowledgeportal.entity.IndexingStatus.FAILED);
            documentRepository.save(document);
            throw e;
        }

        // 5. Save chunks linked to document ID and version 1
        for (TextChunkerService.ChunkDraft draft : chunkDrafts) {
            KnowledgeChunk chunk = new KnowledgeChunk(
                    document.getId(),
                    document.getVersion(),
                    draft.getChunkIndex(),
                    draft.getSectionReference(),
                    draft.getPageNumber(),
                    draft.getContent()
            );
            chunkRepository.save(chunk);
        }

        // 6. Record immutable baseline version snapshot
        String creatorLabel = resolveCreatorWithRole(auth, owner);
        com.example.knowledgeportal.entity.DocumentVersion docVer = new com.example.knowledgeportal.entity.DocumentVersion(
                document.getId(),
                document.getVersion(),
                document.getTitle(),
                document.getCategory(),
                document.getAccessLevel(),
                document.getOriginalFileName(),
                chunkDrafts.size(),
                "Initial document ingestion",
                creatorLabel
        );
        docVer.setCustomCategory(document.getCustomCategory());
        documentVersionRepository.save(docVer);

        // 7. Complete processing job and audit log
        if (job != null) {
            job.setDocumentId(document.getId());
            processingJobService.recordJobSuccess(job.getId());
        }
        auditLogService.recordEvent(owner, "UPLOAD_DOCUMENT", "DOCUMENT", document.getId(), "SUCCESS",
                "Uploaded and indexed document: " + document.getTitle() + " (v1, " + chunkDrafts.size() + " chunks)");

        return new DocumentResponse(document, chunkDrafts.size());
    }

    /**
     * Lists documents accessible to the current user, applying optional filters.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> listAccessibleDocuments(DocumentCategory category, DocumentStatus status,
                                                         AccessLevel accessLevelFilter, Authentication auth) {
        Set<AccessLevel> permittedLevels = permissionService.getAllowedAccessLevels(auth);
        DocumentStatus targetStatus = (status != null) ? status : DocumentStatus.ACTIVE;

        List<KnowledgeDocument> documents;
        if (category != null) {
            documents = documentRepository.findByStatusAndCategoryAndAccessLevelIn(targetStatus, category, permittedLevels);
        } else {
            documents = documentRepository.findByStatusAndAccessLevelIn(targetStatus, permittedLevels);
        }

        // Apply access level filter if user explicitly selected one
        if (accessLevelFilter != null) {
            documents = documents.stream()
                    .filter(d -> d.getAccessLevel() == accessLevelFilter)
                    .collect(Collectors.toList());
        }

        // Non-admin users can only view archived documents that they personally own
        if (targetStatus == DocumentStatus.ARCHIVED && !permissionService.isUserAdmin(auth)) {
            documents = documents.stream()
                    .filter(d -> permissionService.isDocumentOwner(auth, d))
                    .collect(Collectors.toList());
        }

        return documents.stream()
                .map(doc -> {
                    int count = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion()).size();
                    return new DocumentResponse(doc, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves document details and its active version chunks after validating permissions.
     */
    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocumentDetails(Long id, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateDocumentAccess(auth, document);

        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdAndDocumentVersion(document.getId(), document.getVersion());
        List<ChunkResponse> chunkResponses = chunks.stream()
                .map(ChunkResponse::new)
                .collect(Collectors.toList());

        return new DocumentDetailResponse(document, chunkResponses);
    }

    /**
     * Re-indexes a document by re-parsing its content, generating new chunks,
     * and incrementing the document version so obsolete chunks are never returned.
     */
    @Transactional
    public DocumentResponse reindexDocument(Long id, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateManageDocument(auth, document);

        String username = permissionService.getCurrentUsername(auth);
        com.example.knowledgeportal.entity.ProcessingJob job = null;
        try {
            job = processingJobService.recordJobStart(document.getId(), document.getTitle(), document.getVersion() + 1, com.example.knowledgeportal.entity.JobType.REINDEX);
        } catch (Exception ignored) {}

        String textToChunk = document.getSearchableText();

        // If file exists on disk, re-extract freshest content
        if (document.getFileStoragePath() != null) {
            File diskFile = new File(document.getFileStoragePath());
            if (diskFile.exists() && diskFile.length() > 0) {
                textToChunk = documentParserService.parseLocalFile(diskFile, document.getContentType());
                document.setSearchableText(textToChunk);
            }
        }

        if (textToChunk == null || textToChunk.isBlank()) {
            if (job != null) processingJobService.recordJobFailure(job.getId(), "No usable text available to re-index document.");
            throw new InvalidFileException("No usable text available to re-index document.");
        }

        // Increment version (e.g. 1 -> 2)
        document.incrementVersion();
        KnowledgeDocument updatedDoc = documentRepository.save(document);

        // Generate and persist new chunks with the new version number
        List<TextChunkerService.ChunkDraft> chunkDrafts = textChunkerService.splitIntoChunks(textToChunk);
        for (TextChunkerService.ChunkDraft draft : chunkDrafts) {
            KnowledgeChunk chunk = new KnowledgeChunk(
                    updatedDoc.getId(),
                    updatedDoc.getVersion(),
                    draft.getChunkIndex(),
                    draft.getSectionReference(),
                    draft.getPageNumber(),
                    draft.getContent()
            );
            chunkRepository.save(chunk);
        }

        // Record historical snapshot for new version
        String creatorLabel = resolveCreatorWithRole(auth, username);
        com.example.knowledgeportal.entity.DocumentVersion reindexVer = new com.example.knowledgeportal.entity.DocumentVersion(
                updatedDoc.getId(),
                updatedDoc.getVersion(),
                updatedDoc.getTitle(),
                updatedDoc.getCategory(),
                updatedDoc.getAccessLevel(),
                updatedDoc.getOriginalFileName(),
                chunkDrafts.size(),
                "Re-indexed document content",
                creatorLabel
        );
        reindexVer.setCustomCategory(updatedDoc.getCustomCategory());
        documentVersionRepository.save(reindexVer);

        if (job != null) processingJobService.recordJobSuccess(job.getId());
        auditLogService.recordEvent(username, "REINDEX_DOCUMENT", "DOCUMENT", updatedDoc.getId(), "SUCCESS",
                "Re-indexed document: " + updatedDoc.getTitle() + " (v" + updatedDoc.getVersion() + ", " + chunkDrafts.size() + " chunks)");

        return new DocumentResponse(updatedDoc, chunkDrafts.size());
    }

    /**
     * Safely archives a document (soft delete) with permission checks.
     */
    @Transactional
    public DocumentResponse archiveDocument(Long id, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateManageDocument(auth, document);

        document.archive();
        KnowledgeDocument archived = documentRepository.save(document);

        String username = permissionService.getCurrentUsername(auth);
        auditLogService.recordEvent(username, "ARCHIVE_DOCUMENT", "DOCUMENT", archived.getId(), "SUCCESS",
                "Archived document: " + archived.getTitle());

        int count = chunkRepository.findByDocumentIdAndDocumentVersion(archived.getId(), archived.getVersion()).size();
        return new DocumentResponse(archived, count);
    }

    /**
     * Safely restores an archived document back to ACTIVE status.
     */
    @Transactional
    public DocumentResponse restoreDocument(Long id, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateManageDocument(auth, document);

        document.restore();
        KnowledgeDocument restored = documentRepository.save(document);

        String username = permissionService.getCurrentUsername(auth);
        auditLogService.recordEvent(username, "RESTORE_DOCUMENT", "DOCUMENT", restored.getId(), "SUCCESS",
                "Restored archived document: " + restored.getTitle());

        int count = chunkRepository.findByDocumentIdAndDocumentVersion(restored.getId(), restored.getVersion()).size();
        return new DocumentResponse(restored, count);
    }

    /**
     * Updates document metadata and/or replaces content file with automatic version increment.
     */
    @Transactional
    public DocumentResponse updateDocument(Long id, MultipartFile file, String title, String description,
                                           DocumentCategory category, AccessLevel accessLevel,
                                           Authentication auth) {
        return updateDocument(id, file, title, description, category, null, accessLevel, auth);
    }

    @Transactional
    public DocumentResponse updateDocument(Long id, MultipartFile file, String title, String description,
                                           DocumentCategory category, String customCategory, AccessLevel accessLevel,
                                           Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateManageDocument(auth, document);

        if (title != null && !title.trim().isBlank()) {
            document.setTitle(title.trim());
        }
        if (description != null) {
            document.setDescription(description.trim());
        }
        if (category != null) {
            document.setCategory(category);
            if (category == DocumentCategory.OTHER && customCategory != null && !customCategory.trim().isBlank()) {
                document.setCustomCategory(customCategory.trim());
            } else if (category != DocumentCategory.OTHER) {
                document.setCustomCategory(null);
            }
        } else if (customCategory != null) {
            document.setCustomCategory(customCategory.trim());
        }
        if (accessLevel != null) {
            document.setAccessLevel(accessLevel);
        }

        String username = permissionService.getCurrentUsername(auth);
        int chunkCount = chunkRepository.findByDocumentIdAndDocumentVersion(document.getId(), document.getVersion()).size();

        // If replacement file provided, process new version
        if (file != null && !file.isEmpty()) {
            DocumentParserService.ExtractedDocument parsed = documentParserService.parseUploadedFile(file);
            String storedFilePath = storeFileToDisk(file);

            document.incrementVersion();
            document.setOriginalFileName(parsed.getOriginalFilename());
            document.setContentType(parsed.getContentType());
            document.setSearchableText(parsed.getText());
            if (storedFilePath != null) {
                document.setFileStoragePath(storedFilePath);
            }

            List<TextChunkerService.ChunkDraft> chunkDrafts = textChunkerService.splitIntoChunks(parsed.getText());
            for (TextChunkerService.ChunkDraft draft : chunkDrafts) {
                KnowledgeChunk chunk = new KnowledgeChunk(
                        document.getId(),
                        document.getVersion(),
                        draft.getChunkIndex(),
                        draft.getSectionReference(),
                        draft.getPageNumber(),
                        draft.getContent()
                );
                chunkRepository.save(chunk);
            }
            chunkCount = chunkDrafts.size();

            com.example.knowledgeportal.entity.DocumentVersion updateVer = new com.example.knowledgeportal.entity.DocumentVersion(
                    document.getId(),
                    document.getVersion(),
                    document.getTitle(),
                    document.getCategory(),
                    document.getAccessLevel(),
                    document.getOriginalFileName(),
                    chunkCount,
                    "Updated document version and file replacement",
                    resolveCreatorWithRole(auth, username)
            );
            updateVer.setCustomCategory(document.getCustomCategory());
            documentVersionRepository.save(updateVer);
        }

        document.setUpdatedAt(LocalDateTime.now());
        KnowledgeDocument updated = documentRepository.save(document);

        auditLogService.recordEvent(username, "UPDATE_DOCUMENT", "DOCUMENT", updated.getId(), "SUCCESS",
                "Updated document: " + updated.getTitle() + " (v" + updated.getVersion() + ")");

        return new DocumentResponse(updated, chunkCount);
    }

    /**
     * Retrieves historical versions for a document.
     */
    @Transactional(readOnly = true)
    public List<com.example.knowledgeportal.entity.DocumentVersion> getDocumentVersions(Long id, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateDocumentAccess(auth, document);

        List<com.example.knowledgeportal.entity.DocumentVersion> versions =
                documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(id);

        if (versions.isEmpty()) {
            int chunkCount = chunkRepository.findByDocumentIdAndDocumentVersion(document.getId(), document.getVersion()).size();
            String creatorLabel = resolveCreatorWithRole(auth, document.getOwner());
            com.example.knowledgeportal.entity.DocumentVersion initial = new com.example.knowledgeportal.entity.DocumentVersion(
                    document.getId(),
                    document.getVersion(),
                    document.getTitle(),
                    document.getCategory(),
                    document.getAccessLevel(),
                    document.getOriginalFileName(),
                    chunkCount,
                    "Baseline version snapshot",
                    creatorLabel
            );
            initial.setCreatedAt(document.getUploadedAt());
            versions = List.of(initial);
        }

        return versions;
    }

    private String resolveCreatorWithRole(Authentication auth, String fallbackUsername) {
        if (auth != null && auth.getPrincipal() instanceof com.example.knowledgeportal.security.UserPrincipal principal) {
            String roleLabel = "Employee";
            if (principal.getRole() == com.example.knowledgeportal.entity.UserRole.ROLE_ADMIN) {
                roleLabel = "Administrator";
            } else if (principal.getRole() == com.example.knowledgeportal.entity.UserRole.ROLE_HR) {
                roleLabel = "HR Manager";
            }
            return principal.getFullName() + " (" + roleLabel + ")";
        }
        if ("admin_user".equalsIgnoreCase(fallbackUsername)) {
            return "Ashwin Mani (Administrator)";
        } else if ("hr_user".equalsIgnoreCase(fallbackUsername)) {
            return "Suganthi Mani (HR Manager)";
        } else if ("aswini".equalsIgnoreCase(fallbackUsername)) {
            return "Aswini Mani (Senior Software Engineer)";
        } else if ("employee1".equalsIgnoreCase(fallbackUsername)) {
            return "Alice Employee (Software Engineer)";
        }
        return fallbackUsername != null ? fallbackUsername : "System";
    }

    /**
     * Retrieves a single chunk preview after validating document access clearance.
     */
    @Transactional(readOnly = true)
    public ChunkResponse getChunkPreview(Long documentId, Long chunkId, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        permissionService.validateDocumentAccess(auth, document);

        KnowledgeChunk chunk = chunkRepository.findByIdAndDocumentId(chunkId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk not found with ID " + chunkId + " for document ID " + documentId));

        return new ChunkResponse(chunk);
    }

    /**
     * Streams or downloads the complete original document file.
     * Enforces document access security clearance.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getDocumentFile(Long id, Authentication auth) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        permissionService.validateDocumentAccess(auth, document);

        String filename = (document.getOriginalFileName() != null && !document.getOriginalFileName().isBlank())
                ? document.getOriginalFileName()
                : ("document_" + id + (document.getContentType() != null && document.getContentType().contains("pdf") ? ".pdf" : ".txt"));

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        // 1. Try serving stored file from disk
        if (document.getFileStoragePath() != null) {
            File diskFile = new File(document.getFileStoragePath());
            if (diskFile.exists() && diskFile.isFile()) {
                Resource resource = new FileSystemResource(diskFile);
                MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
                if (document.getContentType() != null) {
                    try {
                        mediaType = MediaType.parseMediaType(document.getContentType());
                    } catch (Exception ignored) {}
                }
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                        .body(resource);
            }
        }

        // 2. Fallback to searchableText (e.g. for text documents or seeded documents without disk file)
        String fullText = document.getSearchableText();
        if (fullText != null && !fullText.isBlank()) {
            ByteArrayResource resource = new ByteArrayResource(fullText.getBytes(StandardCharsets.UTF_8));
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        }

        throw new ResourceNotFoundException("Original file content not found for document ID: " + id);
    }

    private String storeFileToDisk(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String uniqueFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetLocation = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toString();
        } catch (IOException e) {
            // Non-fatal if disk write fails in isolated environments, falls back to in-memory/DB text
            return null;
        }
    }
}
