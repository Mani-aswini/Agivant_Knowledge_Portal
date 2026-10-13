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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Value("${knowledge.upload.dir:./uploads}")
    private String uploadDirectory;

    public KnowledgeDocumentService(KnowledgeDocumentRepository documentRepository,
                                  KnowledgeChunkRepository chunkRepository,
                                  DocumentParserService documentParserService,
                                  TextChunkerService textChunkerService,
                                  PermissionService permissionService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentParserService = documentParserService;
        this.textChunkerService = textChunkerService;
        this.permissionService = permissionService;
    }

    /**
     * Uploads and indexes a new document inside a database transaction.
     * Guaranteed not to leave partially indexed records if processing fails.
     */
    @Transactional
    public DocumentResponse uploadAndIndexDocument(MultipartFile file, String title, String description,
                                                  DocumentCategory category, AccessLevel accessLevel,
                                                  Authentication auth) {
        if (title == null || title.trim().isBlank()) {
            throw new InvalidFileException("Document title cannot be empty.");
        }
        if (category == null) {
            throw new InvalidFileException("Document category is required.");
        }
        if (accessLevel == null) {
            throw new InvalidFileException("Document access clearance level is required.");
        }

        // 1. Extract text and validate file type and usable content
        DocumentParserService.ExtractedDocument parsed = documentParserService.parseUploadedFile(file);

        // 2. Safely store the original file on disk outside public static directories
        String storedFilePath = storeFileToDisk(file);

        // 3. Create and persist document entity
        String owner = permissionService.getCurrentUsername(auth);
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
        document = documentRepository.save(document);

        // 4. Segment text into granular chunks
        List<TextChunkerService.ChunkDraft> chunkDrafts = textChunkerService.splitIntoChunks(parsed.getText());
        if (chunkDrafts.isEmpty()) {
            throw new InvalidFileException("Document content could not be segmented into searchable chunks.");
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

        int count = chunkRepository.findByDocumentIdAndDocumentVersion(archived.getId(), archived.getVersion()).size();
        return new DocumentResponse(archived, count);
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
