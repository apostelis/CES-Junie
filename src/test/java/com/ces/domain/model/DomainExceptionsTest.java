package com.ces.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for domain exceptions.
 */
class DomainExceptionsTest {

    // InvalidSessionException tests

    @Test
    void shouldCreateInvalidSessionExceptionWithMessage() {
        // given
        String errorMessage = "Invalid session format";

        // when
        InvalidSessionException exception = new InvalidSessionException(errorMessage);

        // then
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateInvalidSessionExceptionWithMessageAndCause() {
        // given
        String errorMessage = "Session validation failed";
        Throwable cause = new IllegalArgumentException("Invalid format");

        // when
        InvalidSessionException exception = new InvalidSessionException(errorMessage, cause);

        // then
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldBeRuntimeExceptionForInvalidSession() {
        // given
        InvalidSessionException exception = new InvalidSessionException("test");

        // when & then
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void shouldBeThrowableForInvalidSession() {
        // given
        String message = "Session is invalid";

        // when & then
        assertThrows(InvalidSessionException.class, () -> {
            throw new InvalidSessionException(message);
        });
    }

    // SessionNotFoundException tests

    @Test
    void shouldCreateSessionNotFoundExceptionWithMessage() {
        // given
        String errorMessage = "Session does not exist";

        // when
        SessionNotFoundException exception = new SessionNotFoundException(errorMessage);

        // then
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void shouldCreateSessionNotFoundExceptionWithSessionId() {
        // given
        SessionId sessionId = SessionId.of("test-session-123");

        // when
        SessionNotFoundException exception = new SessionNotFoundException(sessionId);

        // then
        assertNotNull(exception);
        assertEquals("Session not found: test-session-123", exception.getMessage());
        assertTrue(exception.getMessage().contains(sessionId.getValue()));
    }

    @Test
    void shouldBeRuntimeExceptionForSessionNotFound() {
        // given
        SessionNotFoundException exception = new SessionNotFoundException("test");

        // when & then
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void shouldBeThrowableForSessionNotFound() {
        // given
        SessionId sessionId = SessionId.generate();

        // when & then
        assertThrows(SessionNotFoundException.class, () -> {
            throw new SessionNotFoundException(sessionId);
        });
    }

    @Test
    void shouldFormatSessionIdInExceptionMessage() {
        // given
        SessionId sessionId = SessionId.of("custom-id-456");

        // when
        SessionNotFoundException exception = new SessionNotFoundException(sessionId);

        // then
        String expectedMessage = "Session not found: custom-id-456";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void shouldAllowCustomMessageInSessionNotFoundException() {
        // given
        String customMessage = "Could not find session in database";

        // when
        SessionNotFoundException exception = new SessionNotFoundException(customMessage);

        // then
        assertEquals(customMessage, exception.getMessage());
        assertFalse(exception.getMessage().contains("Session not found:"));
    }

    @Test
    void shouldPreserveCauseInInvalidSessionException() {
        // given
        NullPointerException cause = new NullPointerException("Null session ID");
        String message = "Validation failed";

        // when
        InvalidSessionException exception = new InvalidSessionException(message, cause);

        // then
        assertEquals(cause, exception.getCause());
        assertEquals("Null session ID", exception.getCause().getMessage());
    }

    @Test
    void shouldAllowNullMessageInInvalidSessionException() {
        // when
        InvalidSessionException exception = new InvalidSessionException(null);

        // then
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void shouldAllowNullMessageInSessionNotFoundException() {
        // when
        SessionNotFoundException exception = new SessionNotFoundException((String) null);

        // then
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void shouldSupportExceptionChaining() {
        // given
        Exception rootCause = new Exception("Root cause");
        InvalidSessionException middleException = new InvalidSessionException("Middle", rootCause);

        // when
        SessionNotFoundException topException = new SessionNotFoundException("Top level error");

        // then - verify exception chaining works
        assertNotNull(middleException.getCause());
        assertEquals(rootCause, middleException.getCause());
        assertNull(topException.getCause());
    }
}
