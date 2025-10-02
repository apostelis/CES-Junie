package com.ces.application.port.output;

import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for session persistence operations.
 * This port defines the contract for storing and retrieving session data.
 */
public interface SessionRepository {

    /**
     * Saves a session to the repository.
     *
     * @param session the session to save
     * @return the saved session
     */
    Session save(Session session);

    /**
     * Finds a session by its ID.
     *
     * @param sessionId the session ID
     * @return an Optional containing the session if found
     */
    Optional<Session> findById(SessionId sessionId);

    /**
     * Finds all active sessions.
     *
     * @return list of active sessions
     */
    List<Session> findAllActive();

    /**
     * Deletes a session by its ID.
     *
     * @param sessionId the session ID
     */
    void deleteById(SessionId sessionId);

    /**
     * Checks if a session exists.
     *
     * @param sessionId the session ID
     * @return true if the session exists
     */
    boolean existsById(SessionId sessionId);
}
