package com.example.knowledgeportal.exception;

/**
 * Thrown when an AI service call times out, encounters an external API error,
 * or fails downstream validation.
 */
public class AiProcessingException extends RuntimeException {
    public AiProcessingException(String message) {
        super(message);
    }

    public AiProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
