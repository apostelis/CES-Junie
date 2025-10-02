package com.ces.application.service;

import com.ces.application.port.input.RegisterSessionUseCase.SessionRegistrationResult;
import com.ces.application.port.output.SessionRepository;
import com.ces.domain.model.InvalidSessionException;
import com.ces.domain.model.Session;
import com.ces.domain.model.SessionId;
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
 * Unit tests for RegisterSessionService.
 */
@ExtendWith(MockitoExtension.class)
class RegisterSessionServiceTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private SessionRepository sessionRepository;

    private RegisterSessionService service;
    private static final String WEBSOCKET_BASE_URL = "ws://localhost:8080/ws";

    @BeforeEach
    void setUp() {
        service = new RegisterSessionService(sessionRegistry, sessionRepository, WEBSOCKET_BASE_URL);
    }

    @Test
    void shouldRegisterNewSessionSuccessfully() {
        // given
        SessionId sessionId = SessionId.generate();
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());

        // when
        SessionRegistrationResult result = service.register(sessionId);

        // then
        assertTrue(result.success());
        assertEquals(sessionId, result.sessionId());
        assertEquals(WEBSOCKET_BASE_URL + "/" + sessionId.getValue(), result.websocketUrl());
        assertEquals("Session registered successfully", result.message());

        verify(sessionRegistry).register(any(Session.class));
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void shouldFailWhenSessionIdIsNull() {
        // when
        SessionRegistrationResult result = service.register(null);

        // then
        assertFalse(result.success());
        assertNull(result.sessionId());
        assertNull(result.websocketUrl());
        assertTrue(result.message().contains("Invalid session"));

        verify(sessionRegistry, never()).register(any(Session.class));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldFailWhenSessionAlreadyExists() {
        // given
        SessionId sessionId = SessionId.generate();
        Session existingSession = new Session(sessionId);
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.of(existingSession));

        // when
        SessionRegistrationResult result = service.register(sessionId);

        // then
        assertFalse(result.success());
        assertEquals(sessionId, result.sessionId());
        assertNull(result.websocketUrl());
        assertTrue(result.message().contains("Invalid session"));
        assertTrue(result.message().contains("already registered"));

        verify(sessionRegistry, never()).register(any(Session.class));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldHandleRegistryException() {
        // given
        SessionId sessionId = SessionId.generate();
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Registry error")).when(sessionRegistry).register(any(Session.class));

        // when
        SessionRegistrationResult result = service.register(sessionId);

        // then
        assertFalse(result.success());
        assertEquals(sessionId, result.sessionId());
        assertNull(result.websocketUrl());
        assertTrue(result.message().contains("Registration failed"));

        verify(sessionRegistry).register(any(Session.class));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldHandleRepositoryException() {
        // given
        SessionId sessionId = SessionId.generate();
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Database error")).when(sessionRepository).save(any(Session.class));

        // when
        SessionRegistrationResult result = service.register(sessionId);

        // then
        assertFalse(result.success());
        assertEquals(sessionId, result.sessionId());
        assertNull(result.websocketUrl());
        assertTrue(result.message().contains("Registration failed"));

        verify(sessionRegistry).register(any(Session.class));
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void shouldBuildCorrectWebSocketUrl() {
        // given
        SessionId sessionId = SessionId.of("test-session-123");
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());

        // when
        SessionRegistrationResult result = service.register(sessionId);

        // then
        assertTrue(result.success());
        assertEquals("ws://localhost:8080/ws/test-session-123", result.websocketUrl());
    }

    @Test
    void shouldThrowExceptionWhenSessionRegistryIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> 
            new RegisterSessionService(null, sessionRepository, WEBSOCKET_BASE_URL)
        );
    }

    @Test
    void shouldThrowExceptionWhenSessionRepositoryIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> 
            new RegisterSessionService(sessionRegistry, null, WEBSOCKET_BASE_URL)
        );
    }

    @Test
    void shouldThrowExceptionWhenWebSocketBaseUrlIsNull() {
        // when & then
        assertThrows(NullPointerException.class, () -> 
            new RegisterSessionService(sessionRegistry, sessionRepository, null)
        );
    }

    @Test
    void shouldRegisterMultipleDifferentSessions() {
        // given
        SessionId sessionId1 = SessionId.generate();
        SessionId sessionId2 = SessionId.generate();
        when(sessionRegistry.findById(any())).thenReturn(Optional.empty());

        // when
        SessionRegistrationResult result1 = service.register(sessionId1);
        SessionRegistrationResult result2 = service.register(sessionId2);

        // then
        assertTrue(result1.success());
        assertTrue(result2.success());
        assertNotEquals(result1.websocketUrl(), result2.websocketUrl());

        verify(sessionRegistry, times(2)).register(any(Session.class));
        verify(sessionRepository, times(2)).save(any(Session.class));
    }

    @Test
    void shouldVerifySessionIsCreatedWithCorrectId() {
        // given
        SessionId sessionId = SessionId.generate();
        when(sessionRegistry.findById(sessionId)).thenReturn(Optional.empty());

        // when
        service.register(sessionId);

        // then
        verify(sessionRegistry).register(argThat(session -> 
            session.getSessionId().equals(sessionId)
        ));
        verify(sessionRepository).save(argThat(session -> 
            session.getSessionId().equals(sessionId)
        ));
    }
}
