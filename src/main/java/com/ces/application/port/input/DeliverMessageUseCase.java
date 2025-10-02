package com.ces.application.port.input;

import com.ces.domain.model.EventMessage;

/**
 * Input port for delivering messages to registered clients.
 * Handles the routing and delivery of Kafka messages to WebSocket connections.
 */
public interface DeliverMessageUseCase {

    /**
     * Delivers a message to the target session.
     *
     * @param message the event message to deliver
     */
    void deliver(EventMessage message);

    /**
     * Delivers a message to all active sessions.
     *
     * @param message the event message to broadcast
     */
    void broadcast(EventMessage message);
}
