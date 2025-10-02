package com.ces.domain.model;

/**
 * Exception thrown when a session ID is invalid or does not meet validation requirements.
 */
public class InvalidSessionException extends RuntimeException {

    public InvalidSessionException(String message) {
        super(message);
    }

    public InvalidSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
