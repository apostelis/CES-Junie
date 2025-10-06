package com.ces.infrastructure.adapter;

import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
import com.ces.domain.service.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of SessionRegistry.
 * Stores active sessions in Redis for distributed session management.
 * Supports both targeted and broadcast message routing.
 */
public class RedisSessionRegistry implements SessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionRegistry.class);
    private static final String SESSION_KEY_PREFIX = "ces:session:";
    private static final long DEFAULT_TTL_HOURS = 24;
    
    private final RedisTemplate<String, Session> redisTemplate;

    public RedisSessionRegistry(RedisTemplate<String, Session> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void register(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        
        String key = buildKey(session.getSessionId());
        
        // Check if session already exists
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException("Session already exists: " + session.getSessionId());
        }
        
        // Store session with TTL
        redisTemplate.opsForValue().set(key, session, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
        logger.debug("Session registered in Redis: {}", session.getSessionId());
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        String key = buildKey(sessionId);
        Session session = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(session);
    }

    @Override
    public boolean isActive(SessionId sessionId) {
        return findById(sessionId)
                .map(Session::isActive)
                .orElse(false);
    }

    @Override
    public void updateHeartbeat(SessionId sessionId) {
        String key = buildKey(sessionId);
        Session session = redisTemplate.opsForValue().get(key);
        
        if (session != null) {
            session.updateHeartbeat();
            // Update session in Redis and refresh TTL
            redisTemplate.opsForValue().set(key, session, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            logger.debug("Heartbeat updated for session in Redis: {}", sessionId);
        }
    }

    @Override
    public void remove(SessionId sessionId) {
        String key = buildKey(sessionId);
        Boolean removed = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(removed)) {
            logger.debug("Session removed from Redis: {}", sessionId);
        }
    }

    @Override
    public int removeExpiredSessions(long timeoutSeconds) {
        Instant expirationThreshold = Instant.now().minusSeconds(timeoutSeconds);
        int removedCount = 0;
        
        // Scan all session keys
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(SESSION_KEY_PREFIX + "*")
                .count(100)
                .build();
        
        try (var cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Session session = redisTemplate.opsForValue().get(key);
                
                if (session != null && session.getLastHeartbeatAt().isBefore(expirationThreshold)) {
                    Boolean deleted = redisTemplate.delete(key);
                    if (Boolean.TRUE.equals(deleted)) {
                        removedCount++;
                        logger.debug("Expired session removed from Redis: {}", session.getSessionId());
                    }
                }
            }
        }
        
        if (removedCount > 0) {
            logger.info("Removed {} expired sessions from Redis", removedCount);
        }
        
        return removedCount;
    }
    
    private String buildKey(SessionId sessionId) {
        return SESSION_KEY_PREFIX + sessionId.getValue();
    }
}
