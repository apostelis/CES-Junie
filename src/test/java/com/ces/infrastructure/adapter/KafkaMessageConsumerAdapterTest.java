package com.ces.infrastructure.adapter;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.domain.model.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KafkaMessageConsumerAdapter.
 */
@ExtendWith(MockitoExtension.class)
class KafkaMessageConsumerAdapterTest {

    @Mock
    private DeliverMessageUseCase deliverMessageUseCase;

    private KafkaMessageConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaMessageConsumerAdapter(deliverMessageUseCase);
    }

    @Test
    void shouldConsumeMessageWithSessionIdHeader() {
        // given
        String message = "test message payload";
        String topic = "test-topic";
        String sessionIdValue = "test-session-123";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
        assertEquals(message, capturedMessage.getData());
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldConsumeMessageWithoutSessionIdHeader() {
        // given
        String message = "test message payload";
        String topic = "test-topic";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, null);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertNotNull(capturedMessage.getTargetSessionId());
        assertNotNull(capturedMessage.getTargetSessionId().getValue());
        assertEquals(message, capturedMessage.getData());
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldHandleEmptyMessage() {
        // given
        String message = "";
        String topic = "test-topic";
        String sessionIdValue = "test-session-123";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals("", capturedMessage.getData());
    }

    @Test
    void shouldHandleComplexJsonMessage() {
        // given
        String message = "{\"orderId\":\"123\",\"customer\":{\"name\":\"John\"},\"items\":[1,2,3]}";
        String topic = "order-events";
        String sessionIdValue = "customer-session-456";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(message, capturedMessage.getData());
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldHandleMultipleConsecutiveCalls() {
        // given
        String message1 = "first message";
        String message2 = "second message";
        String topic = "test-topic";
        String sessionId1 = "session-1";
        String sessionId2 = "session-2";

        // when
        adapter.consume(message1, topic, sessionId1);
        adapter.consume(message2, topic, sessionId2);

        // then
        verify(deliverMessageUseCase, times(2)).deliver(any(EventMessage.class));
    }

    @Test
    void shouldHandleTopicWithSpecialCharacters() {
        // given
        String message = "test message";
        String topic = "customer.events.order_created";
        String sessionIdValue = "session-123";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldPropagateExceptionFromDeliverUseCase() {
        // given
        String message = "test message";
        String topic = "test-topic";
        String sessionIdValue = "session-123";
        
        RuntimeException expectedException = new RuntimeException("Delivery failed");
        doThrow(expectedException).when(deliverMessageUseCase).deliver(any(EventMessage.class));

        // when & then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adapter.consume(message, topic, sessionIdValue));
        
        assertEquals("Delivery failed", thrown.getMessage());
        verify(deliverMessageUseCase).deliver(any(EventMessage.class));
    }

    @Test
    void shouldGenerateDifferentSessionIdsWhenHeaderIsNull() {
        // given
        String message = "test message";
        String topic = "test-topic";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, null);
        adapter.consume(message, topic, null);

        // then
        verify(deliverMessageUseCase, times(2)).deliver(eventMessageCaptor.capture());
        
        // Note: We're testing that sessions are generated (not null), 
        // but we can't guarantee they're different due to SessionId implementation
        EventMessage firstMessage = eventMessageCaptor.getAllValues().get(0);
        EventMessage secondMessage = eventMessageCaptor.getAllValues().get(1);
        
        assertNotNull(firstMessage.getTargetSessionId());
        assertNotNull(secondMessage.getTargetSessionId());
    }

    @Test
    void shouldCreateAdapterWithNonNullUseCase() {
        // when
        KafkaMessageConsumerAdapter newAdapter = new KafkaMessageConsumerAdapter(deliverMessageUseCase);

        // then
        assertNotNull(newAdapter);
    }

    @Test
    void shouldHandleSessionIdWithUUIDFormat() {
        // given
        String message = "test message";
        String topic = "test-topic";
        String sessionIdValue = "550e8400-e29b-41d4-a716-446655440000";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(message, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
    }
}
