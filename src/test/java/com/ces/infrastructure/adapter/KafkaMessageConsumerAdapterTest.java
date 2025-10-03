package com.ces.infrastructure.adapter;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.domain.model.EventMessage;
import com.google.protobuf.Timestamp;
import com.lnw.expressway.messages.v1.FeedMessageProto.FeedMessage;
import com.lnw.expressway.messages.v1.FeedMessageProto.Header;
import com.lnw.expressway.messages.v1.FeedMessageProto.LoginPayload;
import com.lnw.expressway.messages.v1.FeedMessageProto.LogoutPayload;
import com.lnw.expressway.messages.v1.FeedMessageProto.TransPayload;
import com.lnw.expressway.messages.v1.FeedMessageProto.RegistrationPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KafkaMessageConsumerAdapter.
 * Tests the adapter's ability to consume FeedMessage Protobuf messages from Kafka
 * and properly extract session IDs from different payload types.
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

    // Helper methods to create test FeedMessages

    private Timestamp createTimestamp() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    private Header createHeader(Header.MessageType messageType) {
        return Header.newBuilder()
                .setTimestamp(createTimestamp())
                .setMessageType(messageType)
                .setIdentifier(Header.Identifier.newBuilder()
                        .setKey(Header.Identifier.SequencingKey.OPS_Account)
                        .setSequenceId(123456789L)
                        .setUuid("test-uuid-123")
                        .build())
                .setSystemRef(Header.SystemRef.newBuilder()
                        .setProduct(Header.SystemRef.Product.OPS)
                        .setSystem(Header.SystemRef.System.Account)
                        .setTenant("test-tenant")
                        .build())
                .build();
    }

    private FeedMessage createLoginMessage(int accountId) {
        return FeedMessage.newBuilder()
                .setHeader(createHeader(Header.MessageType.Login))
                .setLoginPayload(LoginPayload.newBuilder()
                        .setLoginId(2025010301300000000L)
                        .setAccountId(accountId)
                        .setLoginTime(createTimestamp())
                        .setLogoutTime(createTimestamp())
                        .setIp("192.168.1.1")
                        .setChannel("web")
                        .setIsFailedLogin(false)
                        .build())
                .build();
    }

    private FeedMessage createLogoutMessage(int accountId) {
        return FeedMessage.newBuilder()
                .setHeader(createHeader(Header.MessageType.Logout))
                .setLogoutPayload(LogoutPayload.newBuilder()
                        .setLoginId(2025010301300000000L)
                        .setAccountId(accountId)
                        .setLoginTime(createTimestamp())
                        .setLogoutTime(createTimestamp())
                        .setChannel("web")
                        .build())
                .build();
    }

    private FeedMessage createTransMessage(int accountId) {
        return FeedMessage.newBuilder()
                .setHeader(createHeader(Header.MessageType.WalletTransaction))
                .setTransPayload(TransPayload.newBuilder()
                        .setTransId(2025010301300000000L)
                        .setAccountId(accountId)
                        .setTransType("BET")
                        .setDeltaCash(100.0)
                        .setCurrency("EUR")
                        .setOperatorName("test-operator")
                        .setAccountType(1)
                        .setChannel("web")
                        .setSubsystemId(1)
                        .build())
                .build();
    }

    private FeedMessage createRegistrationMessage(int accountId) {
        return FeedMessage.newBuilder()
                .setHeader(createHeader(Header.MessageType.Registration))
                .setRegistrationPayload(RegistrationPayload.newBuilder()
                        .setAccountId(accountId)
                        .setAccountName("testuser")
                        .build())
                .build();
    }

    // Test cases

    @Test
    void shouldConsumeLoginMessageWithSessionIdHeader() {
        // given
        FeedMessage feedMessage = createLoginMessage(123456789);
        String topic = "test-topic";
        String sessionIdValue = "test-session-123";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
        assertNotNull(capturedMessage.getFeedMessage());
        assertEquals(feedMessage, capturedMessage.getFeedMessage());
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldConsumeLoginMessageWithoutSessionIdHeader() {
        // given - LoginPayload contains account_id for session extraction
        FeedMessage feedMessage = createLoginMessage(987654321);
        String topic = "test-topic";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, null);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertNotNull(capturedMessage.getTargetSessionId());
        // Session ID should be extracted from account_id in payload
        assertEquals("987654321", capturedMessage.getTargetSessionId().getValue());
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldConsumeLogoutMessage() {
        // given
        FeedMessage feedMessage = createLogoutMessage(555555555);
        String topic = "logout-events";
        String sessionIdValue = "session-logout-123";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
        assertEquals(Header.MessageType.Logout, capturedMessage.getFeedMessage().getHeader().getMessageType());
    }

    @Test
    void shouldConsumeTransPayloadMessage() {
        // given
        FeedMessage feedMessage = createTransMessage(111222333);
        String topic = "wallet-transactions";
        String sessionIdValue = "wallet-session-456";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
        assertEquals(Header.MessageType.WalletTransaction, capturedMessage.getFeedMessage().getHeader().getMessageType());
        assertEquals("BET", capturedMessage.getFeedMessage().getTransPayload().getTransType());
    }

    @Test
    void shouldExtractSessionIdFromTransPayloadWhenHeaderMissing() {
        // given
        FeedMessage feedMessage = createTransMessage(444555666);
        String topic = "wallet-transactions";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, null);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        // Session ID should be extracted from account_id in TransPayload
        assertEquals("444555666", capturedMessage.getTargetSessionId().getValue());
    }

    @Test
    void shouldConsumeRegistrationMessage() {
        // given
        FeedMessage feedMessage = createRegistrationMessage(999888777);
        String topic = "registration-events";
        String sessionIdValue = "registration-session";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
        assertEquals(Header.MessageType.Registration, capturedMessage.getFeedMessage().getHeader().getMessageType());
    }

    @Test
    void shouldHandleMultipleConsecutiveMessages() {
        // given
        FeedMessage message1 = createLoginMessage(111111111);
        FeedMessage message2 = createLogoutMessage(222222222);
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
        FeedMessage feedMessage = createLoginMessage(123456789);
        String topic = "customer.events.order_created";
        String sessionIdValue = "session-123";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertEquals(topic, capturedMessage.getSource());
    }

    @Test
    void shouldPropagateExceptionFromDeliverUseCase() {
        // given
        FeedMessage feedMessage = createLoginMessage(123456789);
        String topic = "test-topic";
        String sessionIdValue = "session-123";
        
        RuntimeException expectedException = new RuntimeException("Delivery failed");
        doThrow(expectedException).when(deliverMessageUseCase).deliver(any(EventMessage.class));

        // when & then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adapter.consume(feedMessage, topic, sessionIdValue));
        
        assertEquals("Delivery failed", thrown.getMessage());
        verify(deliverMessageUseCase).deliver(any(EventMessage.class));
    }

    @Test
    void shouldExtractDifferentSessionIdsFromDifferentPayloads() {
        // given
        FeedMessage message1 = createLoginMessage(111111111);
        FeedMessage message2 = createLogoutMessage(222222222);
        String topic = "test-topic";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when - both without session ID header, forcing extraction from payload
        adapter.consume(message1, topic, null);
        adapter.consume(message2, topic, null);

        // then
        verify(deliverMessageUseCase, times(2)).deliver(eventMessageCaptor.capture());
        
        EventMessage firstMessage = eventMessageCaptor.getAllValues().get(0);
        EventMessage secondMessage = eventMessageCaptor.getAllValues().get(1);
        
        assertNotNull(firstMessage.getTargetSessionId());
        assertNotNull(secondMessage.getTargetSessionId());
        assertEquals("111111111", firstMessage.getTargetSessionId().getValue());
        assertEquals("222222222", secondMessage.getTargetSessionId().getValue());
    }

    @Test
    void shouldCreateAdapterWithNonNullUseCase() {
        // when
        KafkaMessageConsumerAdapter newAdapter = new KafkaMessageConsumerAdapter(deliverMessageUseCase);

        // then
        assertNotNull(newAdapter);
    }

    @Test
    void shouldVerifyFeedMessageContainsCorrectHeader() {
        // given
        FeedMessage feedMessage = createLoginMessage(123456789);
        String topic = "test-topic";
        String sessionIdValue = "550e8400-e29b-41d4-a716-446655440000";

        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // when
        adapter.consume(feedMessage, topic, sessionIdValue);

        // then
        verify(deliverMessageUseCase).deliver(eventMessageCaptor.capture());
        
        EventMessage capturedMessage = eventMessageCaptor.getValue();
        assertEquals(sessionIdValue, capturedMessage.getTargetSessionId().getValue());
        
        // Verify FeedMessage structure
        assertNotNull(capturedMessage.getFeedMessage().getHeader());
        assertEquals(Header.MessageType.Login, capturedMessage.getFeedMessage().getHeader().getMessageType());
        assertNotNull(capturedMessage.getFeedMessage().getHeader().getTimestamp());
        assertNotNull(capturedMessage.getFeedMessage().getHeader().getIdentifier());
    }
}
