package com.example.knowledgeportal.exception;

/**
 * Thrown when an uploaded document is empty, corrupted, unsupported,
 * or exceeds allowable limits.
 */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
