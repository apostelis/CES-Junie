package com.ces.infrastructure.adapter;

import com.ces.application.port.output.SessionRepository;
import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SessionRepository.
 * Stores session data in a thread-safe concurrent map.
 */
public class InMemorySessionRepository implements SessionRepository {

    private static final Logger logger = LoggerFactory.getLogger(InMemorySessionRepository.class);
    private final Map<SessionId, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session save(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        sessions.put(session.getSessionId(), session);
        logger.debug("Session saved: {}", session.getSessionId());
        return session;
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public List<Session> findAllActive() {
        return sessions.values().stream()
                .filter(Session::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(SessionId sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.debug("Session deleted: {}", sessionId);
        }
    }

    @Override
    public boolean existsById(SessionId sessionId) {
        return sessions.containsKey(sessionId);
    }
}
