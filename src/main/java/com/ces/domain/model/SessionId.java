package com.ces.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique session identifier.
 * Ensures session IDs are cryptographically secure and non-predictable.
 */
public final class SessionId implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final String value;

    private SessionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }
        this.value = value;
    }

    public static SessionId of(String value) {
        return new SessionId(value);
    }

    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionId sessionId = (SessionId) o;
        return Objects.equals(value, sessionId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
