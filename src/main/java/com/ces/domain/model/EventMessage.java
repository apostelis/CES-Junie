package com.ces.domain.model;

import com.lnw.expressway.messages.v1.FeedMessageProto.FeedMessage;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a message event from Kafka to be delivered to clients.
 * Contains a FeedMessage from the OPS-Events-schema Protobuf definition.
 */
@Getter
public class EventMessage {

    private final String messageId;
    private final SessionId targetSessionId;
    private final FeedMessage feedMessage;
    private final String source;
    private final Instant timestamp;

    public EventMessage(SessionId targetSessionId, FeedMessage feedMessage, String source) {
        this.messageId = UUID.randomUUID().toString();
        this.targetSessionId = Objects.requireNonNull(targetSessionId, "Target session ID cannot be null");
        this.feedMessage = Objects.requireNonNull(feedMessage, "FeedMessage cannot be null");
        this.source = Objects.requireNonNull(source, "Message source cannot be null");
        this.timestamp = Instant.now();
    }

    public EventMessage(String messageId, SessionId targetSessionId, FeedMessage feedMessage, String source, Instant timestamp) {
        this.messageId = Objects.requireNonNull(messageId, "Message ID cannot be null");
        this.targetSessionId = Objects.requireNonNull(targetSessionId, "Target session ID cannot be null");
        this.feedMessage = Objects.requireNonNull(feedMessage, "FeedMessage cannot be null");
        this.source = Objects.requireNonNull(source, "Message source cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }
    
    /**
     * Gets the message data as a JSON string for serialization/delivery.
     * Converts the Protobuf FeedMessage to JSON format.
     */
    public String getData() {
        // For now, return the Protobuf text format
        // In production, you might want to use JsonFormat.printer().print(feedMessage)
        return feedMessage.toString();
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
