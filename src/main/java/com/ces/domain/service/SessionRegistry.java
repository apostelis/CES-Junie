package com.ces.domain.service;

import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;

import java.util.Optional;

/**
 * Domain service interface for managing session registry.
 * This is a domain service that coordinates session lifecycle operations.
 */
public interface SessionRegistry {

    /**
     * Registers a new session.
     *
     * @param session the session to register
     * @throws IllegalArgumentException if session is null or already exists
     */
    void register(Session session);

    /**
     * Retrieves a session by its ID.
     *
     * @param sessionId the session ID
     * @return an Optional containing the session if found
     */
    Optional<Session> findById(SessionId sessionId);

    /**
     * Checks if a session exists and is active.
     *
     * @param sessionId the session ID
     * @return true if the session exists and is active
     */
    boolean isActive(SessionId sessionId);

    /**
     * Updates the heartbeat timestamp for a session.
     *
     * @param sessionId the session ID
     */
    void updateHeartbeat(SessionId sessionId);

    /**
     * Removes a session from the registry.
     *
     * @param sessionId the session ID
     */
    void remove(SessionId sessionId);

    /**
     * Removes expired sessions based on timeout configuration.
     *
     * @param timeoutSeconds the timeout in seconds
     * @return the number of sessions removed
     */
    int removeExpiredSessions(long timeoutSeconds);
}
