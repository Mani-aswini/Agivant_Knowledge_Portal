package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.dto.SearchRequest;
import com.example.knowledgeportal.dto.SearchResponse;
import com.example.knowledgeportal.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Knowledge Base Keyword Search.
 */
@RestController
@RequestMapping("/api/knowledge/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request,
                                                Authentication authentication) {
        SearchResponse response = searchService.search(request, authentication);
        return ResponseEntity.ok(response);
    }
}
