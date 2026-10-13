package com.example.knowledgeportal.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * TextChunkerService segments long document text into indexed chunks.
 * 
 * Beginner Concept:
 * When an AI model or search engine retrieves information, supplying a 100-page
 * document is expensive and noisy. Splitting text into granular passages (chunks)
 * of roughly 400-800 characters allows accurate retrieval of the exact paragraph
 * answering the user's question.
 */
@Service
public class TextChunkerService {

    public static class ChunkDraft {
        private final int chunkIndex;
        private final String sectionReference;
        private final Integer pageNumber;
        private final String content;

        public ChunkDraft(int chunkIndex, String sectionReference, Integer pageNumber, String content) {
            this.chunkIndex = chunkIndex;
            this.sectionReference = sectionReference;
            this.pageNumber = pageNumber;
            this.content = content;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public String getSectionReference() {
            return sectionReference;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public String getContent() {
            return content;
        }
    }

    private static final int TARGET_CHUNK_SIZE = 600;
    private static final int OVERLAP_SIZE = 80;

    /**
     * Splits full document text into structured chunks with section titles and page estimates.
     */
    public List<ChunkDraft> splitIntoChunks(String text) {
        List<ChunkDraft> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Split primarily by paragraphs (double line breaks)
        String[] paragraphs = text.split("\n\s*\n");
        StringBuilder currentBuffer = new StringBuilder();
        String currentSection = "General Section";
        int chunkCounter = 1;
        int charactersProcessed = 0;

        for (String paragraph : paragraphs) {
            String trimmedPara = paragraph.trim();
            if (trimmedPara.isEmpty()) {
                continue;
            }

            // Detect if paragraph looks like a section heading
            if (isSectionHeading(trimmedPara)) {
                // If we already have accumulated text in currentBuffer, flush it first
                if (currentBuffer.length() > 0) {
                    int estimatedPage = Math.max(1, (charactersProcessed / 1800) + 1);
                    chunks.add(new ChunkDraft(chunkCounter++, currentSection, estimatedPage, currentBuffer.toString().trim()));
                    currentBuffer.setLength(0);
                }
                currentSection = cleanHeading(trimmedPara);
                continue;
            }

            // If adding this paragraph exceeds target chunk size, flush current buffer
            if (currentBuffer.length() + trimmedPara.length() > TARGET_CHUNK_SIZE && currentBuffer.length() > 0) {
                int estimatedPage = Math.max(1, (charactersProcessed / 1800) + 1);
                chunks.add(new ChunkDraft(chunkCounter++, currentSection, estimatedPage, currentBuffer.toString().trim()));
                
                // Add minor overlap for context continuity
                String overlap = extractTrailingOverlap(currentBuffer.toString(), OVERLAP_SIZE);
                currentBuffer.setLength(0);
                if (!overlap.isBlank()) {
                    currentBuffer.append(overlap).append("\n");
                }
            }

            if (currentBuffer.length() > 0) {
                currentBuffer.append("\n");
            }
            currentBuffer.append(trimmedPara);
            charactersProcessed += trimmedPara.length();
        }

        // Flush remaining buffer
        if (currentBuffer.length() > 0) {
            int estimatedPage = Math.max(1, (charactersProcessed / 1800) + 1);
            chunks.add(new ChunkDraft(chunkCounter, currentSection, estimatedPage, currentBuffer.toString().trim()));
        }

        // Fallback: If no chunks created (e.g. single giant paragraph without line breaks), segment by character window
        if (chunks.isEmpty() && !text.isBlank()) {
            chunks.addAll(splitByCharacterWindow(text));
        }

        return chunks;
    }

    private boolean isSectionHeading(String paragraph) {
        if (paragraph.length() > 100) {
            return false;
        }
        String lower = paragraph.toLowerCase();
        return lower.startsWith("section ") ||
               lower.startsWith("chapter ") ||
               lower.startsWith("part ") ||
               lower.startsWith("# ") ||
               lower.startsWith("## ") ||
               paragraph.matches("^[0-9]+(\\.[0-9]+)*\\s+.*");
    }

    private String cleanHeading(String heading) {
        return heading.replaceAll("^#+\\s*", "").trim();
    }

    private String extractTrailingOverlap(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return "";
        }
        return text.substring(text.length() - overlapSize).trim();
    }

    private List<ChunkDraft> splitByCharacterWindow(String text) {
        List<ChunkDraft> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        int chunkCounter = 1;

        while (start < length) {
            int end = Math.min(start + TARGET_CHUNK_SIZE, length);
            String snippet = text.substring(start, end).trim();
            int estimatedPage = (start / 1800) + 1;
            chunks.add(new ChunkDraft(chunkCounter++, "Section " + chunkCounter, estimatedPage, snippet));
            start += (TARGET_CHUNK_SIZE - OVERLAP_SIZE);
        }

        return chunks;
    }
}
