package com.ces.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionId value object.
 */
class SessionIdTest {

    @Test
    void shouldCreateSessionIdWithValidValue() {
        // given
        String validValue = "test-session-123";

        // when
        SessionId sessionId = SessionId.of(validValue);

        // then
        assertNotNull(sessionId);
        assertEquals(validValue, sessionId.getValue());
    }

    @Test
    void shouldGenerateSessionIdWithUUID() {
        // when
        SessionId sessionId = SessionId.generate();

        // then
        assertNotNull(sessionId);
        assertNotNull(sessionId.getValue());
        assertFalse(sessionId.getValue().isBlank());
    }

    @Test
    void shouldGenerateUniqueSessionIds() {
        // when
        SessionId sessionId1 = SessionId.generate();
        SessionId sessionId2 = SessionId.generate();

        // then
        assertNotEquals(sessionId1, sessionId2);
        assertNotEquals(sessionId1.getValue(), sessionId2.getValue());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SessionId.of(null)
        );
        assertEquals("Session ID cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenValueIsBlank() {
        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SessionId.of("   ")
        );
        assertEquals("Session ID cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenValueIsEmpty() {
        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SessionId.of("")
        );
        assertEquals("Session ID cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldBeEqualWhenValuesAreTheSame() {
        // given
        String value = "same-session-id";
        SessionId sessionId1 = SessionId.of(value);
        SessionId sessionId2 = SessionId.of(value);

        // when & then
        assertEquals(sessionId1, sessionId2);
        assertEquals(sessionId1.hashCode(), sessionId2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // given
        SessionId sessionId1 = SessionId.of("session-1");
        SessionId sessionId2 = SessionId.of("session-2");

        // when & then
        assertNotEquals(sessionId1, sessionId2);
    }

    @Test
    void shouldReturnValueAsString() {
        // given
        String value = "test-session";
        SessionId sessionId = SessionId.of(value);

        // when
        String result = sessionId.toString();

        // then
        assertEquals(value, result);
    }

    @Test
    void shouldBeEqualToItself() {
        // given
        SessionId sessionId = SessionId.of("test-session");

        // when & then
        assertEquals(sessionId, sessionId);
    }

    @Test
    void shouldNotBeEqualToNull() {
        // given
        SessionId sessionId = SessionId.of("test-session");

        // when & then
        assertNotEquals(null, sessionId);
    }

    @Test
    void shouldNotBeEqualToDifferentClass() {
        // given
        SessionId sessionId = SessionId.of("test-session");
        String differentClass = "test-session";

        // when & then
        assertNotEquals(sessionId, differentClass);
    }
}
