package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.KnowledgeDocument;

import java.util.List;

public class DocumentDetailResponse {

    private DocumentResponse document;
    private List<ChunkResponse> chunks;

    public DocumentDetailResponse() {
    }

    public DocumentDetailResponse(KnowledgeDocument doc, List<ChunkResponse> chunks) {
        this.document = new DocumentResponse(doc, chunks.size());
        this.chunks = chunks;
    }

    public DocumentResponse getDocument() {
        return document;
    }

    public void setDocument(DocumentResponse document) {
        this.document = document;
    }

    public List<ChunkResponse> getChunks() {
        return chunks;
    }

    public void setChunks(List<ChunkResponse> chunks) {
        this.chunks = chunks;
    }
}
