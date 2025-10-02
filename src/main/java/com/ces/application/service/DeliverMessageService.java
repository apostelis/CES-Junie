package com.ces.application.service;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.application.port.output.MessageSender;
import com.ces.domain.model.EventMessage;
import com.ces.domain.model.Session;
import com.ces.domain.model.SessionNotFoundException;
import com.ces.domain.service.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Application service implementing message delivery use case.
 * Routes messages to appropriate sessions via WebSocket connections.
 */
public class DeliverMessageService implements DeliverMessageUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeliverMessageService.class);

    private final SessionRegistry sessionRegistry;
    private final MessageSender messageSender;

    public DeliverMessageService(
            SessionRegistry sessionRegistry,
            MessageSender messageSender) {
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "Session registry cannot be null");
        this.messageSender = Objects.requireNonNull(messageSender, "Message sender cannot be null");
    }

    @Override
    public void deliver(EventMessage message) {
        Objects.requireNonNull(message, "Message cannot be null");

        try {
            // Find target session
            Session session = sessionRegistry.findById(message.getTargetSessionId())
                    .orElseThrow(() -> new SessionNotFoundException(message.getTargetSessionId()));

            // Check if session is active
            if (!session.isActive()) {
                logger.warn("Cannot deliver message to inactive session: {}", message.getTargetSessionId());
                return;
            }

            // Send message via WebSocket
            messageSender.sendToSession(message.getTargetSessionId(), message);
            logger.debug("Message delivered to session: {}", message.getTargetSessionId());

        } catch (SessionNotFoundException e) {
            logger.error("Session not found for message delivery: {}", message.getTargetSessionId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to deliver message to session: {}", message.getTargetSessionId(), e);
            throw new RuntimeException("Message delivery failed", e);
        }
    }

    @Override
    public void broadcast(EventMessage message) {
        Objects.requireNonNull(message, "Message cannot be null");

        try {
            // Send to all active sessions
            messageSender.broadcastToAll(message);
            logger.debug("Message broadcasted to all active sessions");

        } catch (Exception e) {
            logger.error("Failed to broadcast message", e);
            throw new RuntimeException("Message broadcast failed", e);
        }
    }
}
