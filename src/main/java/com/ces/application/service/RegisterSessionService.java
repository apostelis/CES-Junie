package com.ces.application.service;

import com.ces.application.port.input.RegisterSessionUseCase;
import com.ces.application.port.output.SessionRepository;
import com.ces.domain.model.InvalidSessionException;
import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
import com.ces.domain.service.SessionRegistry;

import java.util.Objects;

/**
 * Application service implementing session registration use case.
 * Coordinates between domain services and repositories.
 */
public class RegisterSessionService implements RegisterSessionUseCase {

    private final SessionRegistry sessionRegistry;
    private final SessionRepository sessionRepository;
    private final String websocketBaseUrl;

    public RegisterSessionService(
            SessionRegistry sessionRegistry,
            SessionRepository sessionRepository,
            String websocketBaseUrl) {
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "Session registry cannot be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "Session repository cannot be null");
        this.websocketBaseUrl = Objects.requireNonNull(websocketBaseUrl, "WebSocket base URL cannot be null");
    }

    @Override
    public SessionRegistrationResult register(SessionId sessionId) {
        try {
            validateSessionId(sessionId);

            // Create new session
            Session session = new Session(sessionId);

            // Register in domain registry
            sessionRegistry.register(session);

            // Persist in repository
            sessionRepository.save(session);

            // Build WebSocket URL
            String websocketUrl = buildWebSocketUrl(sessionId);

            return new SessionRegistrationResult(
                    sessionId,
                    websocketUrl,
                    true,
                    "Session registered successfully"
            );

        } catch (InvalidSessionException e) {
            return new SessionRegistrationResult(
                    sessionId,
                    null,
                    false,
                    "Invalid session: " + e.getMessage()
            );
        } catch (Exception e) {
            return new SessionRegistrationResult(
                    sessionId,
                    null,
                    false,
                    "Registration failed: " + e.getMessage()
            );
        }
    }

    private void validateSessionId(SessionId sessionId) {
        if (sessionId == null) {
            throw new InvalidSessionException("Session ID cannot be null");
        }

        // Check if session already exists
        if (sessionRegistry.findById(sessionId).isPresent()) {
            throw new InvalidSessionException("Session already registered: " + sessionId);
        }
    }

    private String buildWebSocketUrl(SessionId sessionId) {
        return websocketBaseUrl + "/" + sessionId.getValue();
    }
}
