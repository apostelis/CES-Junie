package com.ces.integration;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.application.port.input.RegisterSessionUseCase;
import com.ces.application.port.input.RegisterSessionUseCase.SessionRegistrationResult;
import com.ces.application.port.output.MessageSender;
import com.ces.application.port.output.SessionRepository;
import com.ces.application.service.DeliverMessageService;
import com.ces.application.service.RegisterSessionService;
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
import static org.mockito.Mockito.*;

/**
 * Integration tests for session management and message delivery flows.
 * Tests the complete interaction between use cases, domain services, and ports.
 */
@ExtendWith(MockitoExtension.class)
class SessionManagementIntegrationTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MessageSender messageSender;

    private RegisterSessionUseCase registerSessionUseCase;
    private DeliverMessageUseCase deliverMessageUseCase;

    private static final String WEBSOCKET_BASE_URL = "ws://localhost:8080/ws";

    @BeforeEach
    void setUp() {
        registerSessionUseCase = new RegisterSessionService(
                sessionRegistry, 
                sessionRepository, 
                WEBSOCKET_BASE_URL
        );
        
        deliverMessageUseCase = new DeliverMessageService(
                sessionRegistry, 
                messageSender
        );
    }

    @Test
    void shouldCompleteFullSessionRegistrationFlow() {
        // given
        SessionId sessionId = SessionId.generate();
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());

        // when
        SessionRegistrationResult result = registerSessionUseCase.register(sessionId);

        // then
        assertTrue(result.success());
        assertEquals(sessionId, result.sessionId());
        assertNotNull(result.websocketUrl());
        assertTrue(result.websocketUrl().contains(sessionId.getValue()));

        verify(sessionRegistry).register(any(Session.class));
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void shouldRegisterSessionAndDeliverMessage() {
        // given - register session
        SessionId sessionId = SessionId.generate();
        Session registeredSession = new Session(sessionId);
        registeredSession.connect();

        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(registeredSession));
            return null;
        }).when(sessionRegistry).register(any(Session.class));

        // when - register
        SessionRegistrationResult registrationResult = registerSessionUseCase.register(sessionId);

        // then - registration succeeded
        assertTrue(registrationResult.success());

        // given - create message
        EventMessage message = new EventMessage(sessionId, "test data", "kafka-topic");
        when(messageSender.sendToSession(sessionId, message)).thenReturn(true);

        // when - deliver message
        deliverMessageUseCase.deliver(message);

        // then - message delivered
        verify(messageSender).sendToSession(sessionId, message);
    }

    @Test
    void shouldPreventDuplicateSessionRegistration() {
        // given
        SessionId sessionId = SessionId.generate();
        Session existingSession = new Session(sessionId);

        when(sessionRegistry.findById(sessionId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingSession));

        // when - first registration
        SessionRegistrationResult firstResult = registerSessionUseCase.register(sessionId);

        // then - first registration succeeds
        assertTrue(firstResult.success());

        // when - second registration attempt
        SessionRegistrationResult secondResult = registerSessionUseCase.register(sessionId);

        // then - second registration fails
        assertFalse(secondResult.success());
        assertTrue(secondResult.message().contains("already registered"));

        verify(sessionRegistry, times(1)).register(any(Session.class));
    }

    @Test
    void shouldHandleMessageDeliveryToNonExistentSession() {
        // given
        SessionId sessionId = SessionId.generate();
        EventMessage message = new EventMessage(sessionId, "test data", "kafka-topic");

        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(SessionNotFoundException.class, () -> 
            deliverMessageUseCase.deliver(message)
        );

        verify(messageSender, never()).sendToSession(any(), any());
    }

    @Test
    void shouldNotDeliverToInactiveSession() {
        // given
        SessionId sessionId = SessionId.generate();
        Session inactiveSession = new Session(sessionId); // Not connected

        EventMessage message = new EventMessage(sessionId, "test data", "kafka-topic");

        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(inactiveSession));

        // when
        deliverMessageUseCase.deliver(message);

        // then
        verify(messageSender, never()).sendToSession(any(), any());
    }

    @Test
    void shouldBroadcastToAllActiveSessions() {
        // given
        SessionId sessionId = SessionId.generate();
        EventMessage message = new EventMessage(sessionId, "broadcast data", "kafka-topic");

        // when
        deliverMessageUseCase.broadcast(message);

        // then
        verify(messageSender).broadcastToAll(message);
    }

    @Test
    void shouldHandleMultipleSessionsSimultaneously() {
        // given
        SessionId sessionId1 = SessionId.generate();
        SessionId sessionId2 = SessionId.generate();
        SessionId sessionId3 = SessionId.generate();

        when(sessionRegistry.findById(any())).thenReturn(Optional.empty());

        // when - register multiple sessions
        SessionRegistrationResult result1 = registerSessionUseCase.register(sessionId1);
        SessionRegistrationResult result2 = registerSessionUseCase.register(sessionId2);
        SessionRegistrationResult result3 = registerSessionUseCase.register(sessionId3);

        // then - all registrations succeed
        assertTrue(result1.success());
        assertTrue(result2.success());
        assertTrue(result3.success());

        assertNotEquals(result1.websocketUrl(), result2.websocketUrl());
        assertNotEquals(result2.websocketUrl(), result3.websocketUrl());

        verify(sessionRegistry, times(3)).register(any(Session.class));
        verify(sessionRepository, times(3)).save(any(Session.class));
    }

    @Test
    void shouldHandleSessionLifecycleFromRegistrationToDisconnection() {
        // given
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId);

        when(sessionRegistry.findById(sessionId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(session));

        // when - register session
        SessionRegistrationResult registrationResult = registerSessionUseCase.register(sessionId);

        // then - registration successful
        assertTrue(registrationResult.success());

        // when - connect session
        session.connect();
        assertTrue(session.isActive());

        // given - message for active session
        EventMessage message = new EventMessage(sessionId, "test data", "kafka-topic");
        when(messageSender.sendToSession(sessionId, message)).thenReturn(true);

        // when - deliver to active session
        deliverMessageUseCase.deliver(message);

        // then - message delivered
        verify(messageSender).sendToSession(sessionId, message);

        // when - disconnect session
        session.disconnect();
        assertFalse(session.isActive());

        // when - try to deliver to disconnected session
        EventMessage message2 = new EventMessage(sessionId, "test data 2", "kafka-topic");
        deliverMessageUseCase.deliver(message2);

        // then - message not delivered to disconnected session
        verify(messageSender, never()).sendToSession(sessionId, message2);
    }

    @Test
    void shouldHandleErrorsGracefullyDuringRegistration() {
        // given
        SessionId sessionId = SessionId.generate();
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Database unavailable"))
                .when(sessionRepository).save(any(Session.class));

        // when
        SessionRegistrationResult result = registerSessionUseCase.register(sessionId);

        // then
        assertFalse(result.success());
        assertTrue(result.message().contains("Registration failed"));

        verify(sessionRegistry).register(any(Session.class));
    }

    @Test
    void shouldHandleErrorsGracefullyDuringMessageDelivery() {
        // given
        SessionId sessionId = SessionId.generate();
        Session activeSession = new Session(sessionId);
        activeSession.connect();

        EventMessage message = new EventMessage(sessionId, "test data", "kafka-topic");

        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(messageSender.sendToSession(sessionId, message))
                .thenThrow(new RuntimeException("WebSocket connection lost"));

        // when & then
        assertThrows(RuntimeException.class, () -> 
            deliverMessageUseCase.deliver(message)
        );

        verify(messageSender).sendToSession(sessionId, message);
    }

    @Test
    void shouldValidateSessionIdFormat() {
        // given - null session ID
        // when
        SessionRegistrationResult result = registerSessionUseCase.register(null);

        // then
        assertFalse(result.success());
        assertTrue(result.message().contains("Invalid session"));

        verify(sessionRegistry, never()).register(any(Session.class));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldDeliverMessageWithCorrectMetadata() {
        // given
        SessionId sessionId = SessionId.generate();
        Session activeSession = new Session(sessionId);
        activeSession.connect();

        String testData = "{\"key\": \"value\"}";
        String testSource = "customer-events-topic";
        EventMessage message = new EventMessage(sessionId, testData, testSource);

        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(messageSender.sendToSession(sessionId, message)).thenReturn(true);

        // when
        deliverMessageUseCase.deliver(message);

        // then
        verify(messageSender).sendToSession(eq(sessionId), argThat(msg ->
                msg.getData().equals(testData) &&
                msg.getSource().equals(testSource) &&
                msg.getTargetSessionId().equals(sessionId)
        ));
    }
}
