package com.ces.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a client session.
 * Tracks session lifecycle, connection status, and heartbeat information.
 */
public class Session {

    private final SessionId sessionId;
    private SessionStatus status;
    private final Instant createdAt;
    private Instant lastHeartbeatAt;
    private Instant disconnectedAt;

    public Session(SessionId sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
        this.status = SessionStatus.REGISTERED;
        this.createdAt = Instant.now();
        this.lastHeartbeatAt = Instant.now();
    }

    public void connect() {
        if (status == SessionStatus.DISCONNECTED) {
            this.status = SessionStatus.CONNECTED;
            this.disconnectedAt = null;
        } else if (status == SessionStatus.REGISTERED) {
            this.status = SessionStatus.CONNECTED;
        }
    }

    public void disconnect() {
        this.status = SessionStatus.DISCONNECTED;
        this.disconnectedAt = Instant.now();
    }

    public void updateHeartbeat() {
        this.lastHeartbeatAt = Instant.now();
    }

    public boolean isActive() {
        return status == SessionStatus.CONNECTED;
    }

    public boolean isExpired(long timeoutSeconds) {
        if (lastHeartbeatAt == null) {
            return false;
        }
        Instant timeout = lastHeartbeatAt.plusSeconds(timeoutSeconds);
        return Instant.now().isAfter(timeout);
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(sessionId, session.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId=" + sessionId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", lastHeartbeatAt=" + lastHeartbeatAt +
                '}';
    }
}
