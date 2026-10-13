package com.example.knowledgeportal.dto;

import java.util.List;

public class SearchResponse {

    private String query;
    private int totalResults;
    private List<SearchResultItem> results;

    public SearchResponse() {
    }

    public SearchResponse(String query, int totalResults, List<SearchResultItem> results) {
        this.query = query;
        this.totalResults = totalResults;
        this.results = results;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<SearchResultItem> getResults() {
        return results;
    }

    public void setResults(List<SearchResultItem> results) {
        this.results = results;
    }
}
