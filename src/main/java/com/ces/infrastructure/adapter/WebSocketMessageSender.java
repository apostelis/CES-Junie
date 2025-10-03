package com.ces.infrastructure.adapter;

import com.ces.application.port.output.MessageSender;
import com.ces.domain.model.EventMessage;
import com.ces.domain.model.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket-based implementation of MessageSender.
 * Manages WebSocket connections and message delivery.
 */
public class WebSocketMessageSender implements MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageSender.class);
    private final Map<SessionId, Object> connections = new ConcurrentHashMap<>();

    @Override
    public boolean sendToSession(SessionId sessionId, EventMessage message) {
        if (sessionId == null || message == null) {
            logger.warn("Cannot send message: sessionId or message is null");
            return false;
        }

        // Check if connection exists
        if (!connections.containsKey(sessionId)) {
            logger.warn("No WebSocket connection for session: {}", sessionId);
            return false;
        }

        // TODO: Implement actual WebSocket message sending
        logger.debug("Message sent to session {}: {}", sessionId, message);
        return true;
    }

    @Override
    public void broadcastToAll(EventMessage message) {
        if (message == null) {
            logger.warn("Cannot broadcast: message is null");
            return;
        }

        logger.debug("Broadcasting message to {} sessions", connections.size());
        
        for (SessionId sessionId : connections.keySet()) {
            sendToSession(sessionId, message);
        }
    }

    @Override
    public boolean isConnected(SessionId sessionId) {
        return connections.containsKey(sessionId);
    }

    /**
     * Registers a WebSocket connection for a session.
     * 
     * @param sessionId the session ID
     * @param connection the WebSocket connection object
     */
    public void registerConnection(SessionId sessionId, Object connection) {
        connections.put(sessionId, connection);
        logger.debug("WebSocket connection registered for session: {}", sessionId);
    }

    /**
     * Removes a WebSocket connection for a session.
     * 
     * @param sessionId the session ID
     */
    public void removeConnection(SessionId sessionId) {
        connections.remove(sessionId);
        logger.debug("WebSocket connection removed for session: {}", sessionId);
    }
}
