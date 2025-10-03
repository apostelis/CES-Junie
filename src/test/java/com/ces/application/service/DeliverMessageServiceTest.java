package com.ces.application.service;

import com.ces.application.port.output.MessageSender;
import com.ces.domain.model.EventMessage;
import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
import com.ces.domain.model.SessionNotFoundException;
import com.ces.domain.service.SessionRegistry;
import com.google.protobuf.Timestamp;
import com.lnw.expressway.messages.v1.FeedMessageProto.FeedMessage;
import com.lnw.expressway.messages.v1.FeedMessageProto.Header;
import com.lnw.expressway.messages.v1.FeedMessageProto.LoginPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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

    // Helper method to create test FeedMessage
    private FeedMessage createTestFeedMessage(int accountId) {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        return FeedMessage.newBuilder()
                .setHeader(Header.newBuilder()
                        .setTimestamp(timestamp)
                        .setMessageType(Header.MessageType.Login)
                        .setIdentifier(Header.Identifier.newBuilder()
                                .setKey(Header.Identifier.SequencingKey.OPS_Account)
                                .setSequenceId(123456789L)
                                .setUuid("test-uuid")
                                .build())
                        .setSystemRef(Header.SystemRef.newBuilder()
                                .setProduct(Header.SystemRef.Product.OPS)
                                .setSystem(Header.SystemRef.System.Account)
                                .setTenant("test-tenant")
                                .build())
                        .build())
                .setLoginPayload(LoginPayload.newBuilder()
                        .setLoginId(2025010301300000000L)
                        .setAccountId(accountId)
                        .setLoginTime(timestamp)
                        .setLogoutTime(timestamp)
                        .setIp("192.168.1.1")
                        .setChannel("web")
                        .setIsFailedLogin(false)
                        .build())
                .build();
    }

    @Test
    void shouldDeliverMessageToActiveSession() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId);
        session.connect();
        
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
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
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
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
        
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
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
        
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
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
        
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
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
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");

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
        FeedMessage feedMessage = createTestFeedMessage(123456789);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
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
        
        FeedMessage feedMessage1 = createTestFeedMessage(111111111);
        FeedMessage feedMessage2 = createTestFeedMessage(222222222);
        EventMessage message1 = new EventMessage(sessionId, feedMessage1, "topic-1");
        EventMessage message2 = new EventMessage(sessionId, feedMessage2, "topic-2");
        
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
        
        FeedMessage feedMessage1 = createTestFeedMessage(333333333);
        FeedMessage feedMessage2 = createTestFeedMessage(444444444);
        EventMessage message1 = new EventMessage(sessionId1, feedMessage1, "topic-1");
        EventMessage message2 = new EventMessage(sessionId2, feedMessage2, "topic-2");
        
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
        
        FeedMessage feedMessage = createTestFeedMessage(555555555);
        EventMessage message = new EventMessage(sessionId, feedMessage, "test-topic");
        
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(registeredSession));

        // when
        service.deliver(message);

        // then
        assertFalse(registeredSession.isActive());
        verify(messageSender, never()).sendToSession(any(), any());
    }
}
