package com.example.knowledgeportal.service;

import com.example.knowledgeportal.exception.InvalidFileException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * DocumentParserService handles validation and text extraction for uploaded files.
 * 
 * Supported Formats:
 * 1. Plain Text (.txt, text/plain)
 * 2. Adobe PDF (.pdf, application/pdf) using Apache PDFBox
 * 
 * Beginner Concept:
 * Text extraction extracts the raw textual characters from formatted file formats.
 * Notice: Scanned PDFs without an embedded text layer (pure image scans) contain no
 * extractable characters unless OCR is installed. If an uploaded PDF yields empty text,
 * it is rejected as unprocessable.
 */
@Service
public class DocumentParserService {

    @Value("${knowledge.upload.max-size-bytes:10485760}")
    private long maxFileSizeBytes; // 10MB default

    public static class ExtractedDocument {
        private final String text;
        private final String contentType;
        private final String originalFilename;

        public ExtractedDocument(String text, String contentType, String originalFilename) {
            this.text = text;
            this.contentType = contentType;
            this.originalFilename = originalFilename;
        }

        public String getText() {
            return text;
        }

        public String getContentType() {
            return contentType;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }
    }

    /**
     * Extracts text from an incoming MultipartFile upload after strict validation.
     */
    public ExtractedDocument parseUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty or missing. Please select a valid document.");
        }

        // 1. Enforce configurable file size limit
        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException("File size (" + (file.getSize() / 1024) + " KB) exceeds maximum allowed limit of " +
                    (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("Uploaded file name is invalid.");
        }

        String lowerFilename = filename.toLowerCase();
        String contentType = file.getContentType();
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException("Failed to read uploaded file: " + e.getMessage());
        }

        String extractedText;

        try {
            if (lowerFilename.endsWith(".txt")) {
                // Strict check: plain text must not contain binary null bytes (0x00)
                for (int i = 0; i < Math.min(fileBytes.length, 1024); i++) {
                    if (fileBytes[i] == 0) {
                        throw new InvalidFileException("Spoofed file detected: File has .txt extension but contains binary executable content.");
                    }
                }
                extractedText = new String(fileBytes, StandardCharsets.UTF_8);
                if (contentType == null || contentType.isBlank()) {
                    contentType = "text/plain";
                }
            } else if (lowerFilename.endsWith(".pdf")) {
                // Magic-byte check: Adobe PDF must start with '%PDF-'
                if (fileBytes.length < 5) {
                    throw new InvalidFileException("File is too small to be a valid PDF document.");
                }
                String header = new String(fileBytes, 0, Math.min(fileBytes.length, 10), StandardCharsets.US_ASCII);
                if (!header.startsWith("%PDF")) {
                    throw new InvalidFileException("Spoofed file detected: File has .pdf extension but lacks a valid PDF header (%PDF-).");
                }
                extractedText = extractTextFromPdf(new ByteArrayInputStream(fileBytes));
                if (contentType == null || contentType.isBlank()) {
                    contentType = "application/pdf";
                }
            } else {
                throw new InvalidFileException("Unsupported file format: '" + filename +
                        "'. Only plain text (.txt) and PDF (.pdf) documents are supported.");
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to read uploaded file: " + e.getMessage());
        }

        String cleanedText = cleanExtractedText(extractedText);
        if (cleanedText.isBlank()) {
            throw new InvalidFileException("The document contains no usable extractable text. Note that scanned image-only PDFs require OCR which is not enabled.");
        }

        return new ExtractedDocument(cleanedText, contentType, filename);
    }

    /**
     * Extracts text from a stored file on disk during re-indexing.
     */
    public String parseLocalFile(File file, String contentType) {
        if (file == null || !file.exists() || file.length() == 0) {
            throw new InvalidFileException("Stored document file is missing or empty.");
        }

        String lowerName = file.getName().toLowerCase();
        String extractedText;

        try {
            if (lowerName.endsWith(".txt") || "text/plain".equalsIgnoreCase(contentType)) {
                extractedText = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } else if (lowerName.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(contentType)) {
                try (PDDocument document = PDDocument.load(file)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    extractedText = stripper.getText(document);
                }
            } else {
                throw new InvalidFileException("Unsupported stored file format: " + file.getName());
            }
        } catch (IOException e) {
            throw new InvalidFileException("Error extracting text from stored file: " + e.getMessage());
        }

        String cleanedText = cleanExtractedText(extractedText);
        if (cleanedText.isBlank()) {
            throw new InvalidFileException("Stored file yielded no usable text during re-indexing.");
        }

        return cleanedText;
    }

    private String extractTextFromTxt(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new InvalidFileException("Encrypted or password-protected PDF files cannot be processed.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }
        // Normalize line breaks and remove non-printable control characters
        String normalized = text.replace("\r\n", "\n")
                                .replace("\r", "\n")
                                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // Remove table-of-contents dot trails (e.g. "................")
        normalized = normalized.replaceAll("\\.{3,}", " ")
                               // Remove page number markers like "Page 2 of 11"
                               .replaceAll("(?i)Page\\s+\\d+\\s+of\\s+\\d+", "")
                               // Remove repeated dashes or underline trails
                               .replaceAll("_{3,}", " ")
                               // Collapse excessive spaces and tabs on the same line
                               .replaceAll("[ \\t]{2,}", " ");

        // Remove lines that became purely whitespace or punctuation
        StringBuilder sb = new StringBuilder();
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.matches("^[\\.\\-_\\s]+$")) {
                sb.append(trimmed).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
