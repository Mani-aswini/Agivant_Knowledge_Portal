package com.example.knowledgeportal.exception;

/**
 * Thrown when an authenticated user attempts to access a document or operation
 * beyond their clearance level.
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
