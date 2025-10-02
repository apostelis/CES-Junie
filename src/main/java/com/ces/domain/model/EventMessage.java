package com.ces.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a message event from Kafka to be delivered to clients.
 */
@Getter
public class EventMessage {

    private final String messageId;
    private final SessionId targetSessionId;
    private final String data;
    private final String source;
    private final Instant timestamp;

    public EventMessage(SessionId targetSessionId, String data, String source) {
        this.messageId = UUID.randomUUID().toString();
        this.targetSessionId = Objects.requireNonNull(targetSessionId, "Target session ID cannot be null");
        this.data = Objects.requireNonNull(data, "Message data cannot be null");
        this.source = Objects.requireNonNull(source, "Message source cannot be null");
        this.timestamp = Instant.now();
    }

    public EventMessage(String messageId, SessionId targetSessionId, String data, String source, Instant timestamp) {
        this.messageId = Objects.requireNonNull(messageId, "Message ID cannot be null");
        this.targetSessionId = Objects.requireNonNull(targetSessionId, "Target session ID cannot be null");
        this.data = Objects.requireNonNull(data, "Message data cannot be null");
        this.source = Objects.requireNonNull(source, "Message source cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventMessage that = (EventMessage) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "EventMessage{" +
                "messageId='" + messageId + '\'' +
                ", targetSessionId=" + targetSessionId +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
