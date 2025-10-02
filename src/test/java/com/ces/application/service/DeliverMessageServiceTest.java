package com.ces.application.service;

import com.ces.application.port.output.MessageSender;
import com.ces.domain.model.EventMessage;
import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
import com.ces.domain.model.SessionNotFoundException;
import com.ces.domain.service.SessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeliverMessageService.
 */
@ExtendWith(MockitoExtension.class)
class DeliverMessageServiceTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private MessageSender messageSender;

    private DeliverMessageService service;

    @BeforeEach
    void setUp() {
        service = new DeliverMessageService(sessionRegistry, messageSender);
    }

    @Test
    void shouldDeliverMessageToActiveSession() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId);
        session.connect();
        
        EventMessage message = new EventMessage(sessionId, "test data", "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageSender.sendToSession(sessionId, message)).thenReturn(true);

        // when
        service.deliver(message);

        // then
        verify(sessionRegistry).findById(sessionId);
        verify(messageSender).sendToSession(sessionId, message);
    }

    @Test
    void shouldThrowExceptionWhenSessionNotFound() {
        // given
        SessionId sessionId = SessionId.generate();
        EventMessage message = new EventMessage(sessionId, "test data", "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(SessionNotFoundException.class, () -> service.deliver(message));
        
        verify(sessionRegistry).findById(sessionId);
        verify(messageSender, never()).sendToSession(any(), any());
    }

    @Test
    void shouldNotDeliverMessageToInactiveSession() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId); // Not connected, so inactive
        
        EventMessage message = new EventMessage(sessionId, "test data", "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(session));

        // when
        service.deliver(message);

        // then
        verify(sessionRegistry).findById(sessionId);
        verify(messageSender, never()).sendToSession(any(), any());
    }

    @Test
    void shouldNotDeliverMessageToDisconnectedSession() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId);
        session.connect();
        session.disconnect();
        
        EventMessage message = new EventMessage(sessionId, "test data", "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(session));

        // when
        service.deliver(message);

        // then
        verify(sessionRegistry).findById(sessionId);
        verify(messageSender, never()).sendToSession(any(), any());
    }

    @Test
    void shouldThrowExceptionWhenMessageIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> service.deliver(null));
        
        verify(sessionRegistry, never()).findById(any());
        verify(messageSender, never()).sendToSession(any(), any());
    }

    @Test
    void shouldHandleMessageSenderException() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId);
        session.connect();
        
        EventMessage message = new EventMessage(sessionId, "test data", "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageSender.sendToSession(sessionId, message))
            .thenThrow(new RuntimeException("WebSocket error"));

        // when & then
        assertThrows(RuntimeException.class, () -> service.deliver(message));
        
        verify(sessionRegistry).findById(sessionId);
        verify(messageSender).sendToSession(sessionId, message);
    }

    @Test
    void shouldBroadcastMessageToAllSessions() {
        // given
        SessionId sessionId = SessionId.generate();
        EventMessage message = new EventMessage(sessionId, "broadcast data", "test-topic");

        // when
        service.broadcast(message);

        // then
        verify(messageSender).broadcastToAll(message);
    }

    @Test
    void shouldThrowExceptionWhenBroadcastMessageIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> service.broadcast(null));
        
        verify(messageSender, never()).broadcastToAll(any());
    }

    @Test
    void shouldHandleBroadcastException() {
        // given
        SessionId sessionId = SessionId.generate();
        EventMessage message = new EventMessage(sessionId, "broadcast data", "test-topic");
        
        doThrow(new RuntimeException("Broadcast failed")).when(messageSender).broadcastToAll(message);

        // when & then
        assertThrows(RuntimeException.class, () -> service.broadcast(message));
        
        verify(messageSender).broadcastToAll(message);
    }

    @Test
    void shouldThrowExceptionWhenSessionRegistryIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> 
            new DeliverMessageService(null, messageSender)
        );
    }

    @Test
    void shouldThrowExceptionWhenMessageSenderIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> 
            new DeliverMessageService(sessionRegistry, null)
        );
    }

    @Test
    void shouldDeliverMultipleMessagesToSameSession() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId);
        session.connect();
        
        EventMessage message1 = new EventMessage(sessionId, "data 1", "topic-1");
        EventMessage message2 = new EventMessage(sessionId, "data 2", "topic-2");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageSender.sendToSession(eq(sessionId), any())).thenReturn(true);

        // when
        service.deliver(message1);
        service.deliver(message2);

        // then
        verify(sessionRegistry, times(2)).findById(sessionId);
        verify(messageSender).sendToSession(sessionId, message1);
        verify(messageSender).sendToSession(sessionId, message2);
    }

    @Test
    void shouldDeliverMessagesToDifferentSessions() {
        // given
        SessionId sessionId1 = SessionId.generate();
        SessionId sessionId2 = SessionId.generate();
        
        Session session1 = new Session(sessionId1);
        session1.connect();
        Session session2 = new Session(sessionId2);
        session2.connect();
        
        EventMessage message1 = new EventMessage(sessionId1, "data 1", "topic-1");
        EventMessage message2 = new EventMessage(sessionId2, "data 2", "topic-2");
        
        when(sessionRegistry.findById(sessionId1)).thenReturn(Optional.of(session1));
        when(sessionRegistry.findById(sessionId2)).thenReturn(Optional.of(session2));
        when(messageSender.sendToSession(any(), any())).thenReturn(true);

        // when
        service.deliver(message1);
        service.deliver(message2);

        // then
        verify(messageSender).sendToSession(sessionId1, message1);
        verify(messageSender).sendToSession(sessionId2, message2);
    }

    @Test
    void shouldVerifySessionIsActiveBeforeDelivery() {
        // given
        SessionId sessionId = SessionId.generate();
        Session registeredSession = new Session(sessionId); // REGISTERED status, not active
        
        EventMessage message = new EventMessage(sessionId, "test data", "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(registeredSession));

        // when
        service.deliver(message);

        // then
        assertFalse(registeredSession.isActive());
        verify(messageSender, never()).sendToSession(any(), any());
    }
}
