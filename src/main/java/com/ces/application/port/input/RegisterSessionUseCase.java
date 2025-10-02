package com.ces.application.port.input;

import com.ces.domain.model.SessionId;

/**
 * Input port for session registration use case.
 * Defines the contract for registering new client sessions.
 */
public interface RegisterSessionUseCase {

    /**
     * Registers a new session with the provided session ID.
     *
     * @param sessionId the session ID to register
     * @return the registered session information
     * @throws com.ces.domain.model.InvalidSessionException if the session ID is invalid
     */
    SessionRegistrationResult register(SessionId sessionId);

    /**
     * Result of a session registration operation.
     */
    record SessionRegistrationResult(
            SessionId sessionId,
            String websocketUrl,
            boolean success,
            String message
    ) {}
}
