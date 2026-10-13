package com.example.knowledgeportal.exception;

/**
 * Thrown when a requested resource (e.g. document, chunk) is not found in the database.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
