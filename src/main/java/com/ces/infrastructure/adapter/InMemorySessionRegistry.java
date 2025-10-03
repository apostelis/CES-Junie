package com.ces.infrastructure.adapter;

import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
import com.ces.domain.service.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SessionRegistry.
 * Stores active sessions in a thread-safe concurrent map.
 */
public class InMemorySessionRegistry implements SessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(InMemorySessionRegistry.class);
    private final Map<SessionId, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void register(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        if (sessions.containsKey(session.getSessionId())) {
            throw new IllegalArgumentException("Session already exists: " + session.getSessionId());
        }
        sessions.put(session.getSessionId(), session);
        logger.debug("Session registered: {}", session.getSessionId());
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public boolean isActive(SessionId sessionId) {
        return findById(sessionId)
                .map(Session::isActive)
                .orElse(false);
    }

    @Override
    public void updateHeartbeat(SessionId sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.updateHeartbeat();
            logger.debug("Heartbeat updated for session: {}", sessionId);
        }
    }

    @Override
    public void remove(SessionId sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.debug("Session removed: {}", sessionId);
        }
    }

    @Override
    public int removeExpiredSessions(long timeoutSeconds) {
        Instant expirationThreshold = Instant.now().minusSeconds(timeoutSeconds);
        int removedCount = 0;
        
        for (Map.Entry<SessionId, Session> entry : sessions.entrySet()) {
            if (entry.getValue().getLastHeartbeatAt().isBefore(expirationThreshold)) {
                sessions.remove(entry.getKey());
                removedCount++;
                logger.debug("Expired session removed: {}", entry.getKey());
            }
        }
        
        if (removedCount > 0) {
            logger.info("Removed {} expired sessions", removedCount);
        }
        
        return removedCount;
    }
}
