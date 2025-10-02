package com.ces.domain.model;

/**
 * Exception thrown when a session cannot be found in the registry.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(SessionId sessionId) {
        super("Session not found: " + sessionId);
    }
}
