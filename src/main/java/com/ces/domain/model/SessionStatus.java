package com.ces.domain.model;

/**
 * Enumeration of possible session states.
 */
public enum SessionStatus {
    /**
     * Session is registered but not yet connected via WebSocket.
     */
    REGISTERED,

    /**
     * Session is actively connected via WebSocket.
     */
    CONNECTED,

    /**
     * Session has been disconnected.
     */
    DISCONNECTED
}
