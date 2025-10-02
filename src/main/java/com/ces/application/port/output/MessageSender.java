package com.ces.application.port.output;

import com.ces.domain.model.EventMessage;
import com.ces.domain.model.SessionId;

/**
 * Output port for sending messages to clients via WebSocket.
 * This port abstracts the WebSocket communication mechanism.
 */
public interface MessageSender {

    /**
     * Sends a message to a specific session.
     *
     * @param sessionId the target session ID
     * @param message the message to send
     * @return true if the message was sent successfully
     */
    boolean sendToSession(SessionId sessionId, EventMessage message);

    /**
     * Sends a message to all connected sessions.
     *
     * @param message the message to broadcast
     */
    void broadcastToAll(EventMessage message);

    /**
     * Checks if a session has an active WebSocket connection.
     *
     * @param sessionId the session ID
     * @return true if the session is connected
     */
    boolean isConnected(SessionId sessionId);
}
