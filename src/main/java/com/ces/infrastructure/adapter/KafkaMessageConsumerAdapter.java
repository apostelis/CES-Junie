package com.ces.infrastructure.adapter;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.domain.model.EventMessage;
import com.ces.domain.model.SessionId;
import com.lnw.expressway.messages.v1.FeedMessageProto.FeedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter that listens to configured topics and delivers messages
 * to registered sessions through the domain layer.
 * This adapter bridges the Kafka infrastructure with the application's hexagonal architecture.
 * Consumes Protobuf FeedMessage events from the OPS-Events-schema.
 */
@Component
public class KafkaMessageConsumerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageConsumerAdapter.class);
    
    private final DeliverMessageUseCase deliverMessageUseCase;
    
    public KafkaMessageConsumerAdapter(DeliverMessageUseCase deliverMessageUseCase) {
        this.deliverMessageUseCase = deliverMessageUseCase;
    }
    
    /**
     * Consumes Protobuf FeedMessage from configured Kafka topics.
     * The topics are configured in application.yml under ces.kafka.topics.
     * 
     * @param feedMessage the Protobuf FeedMessage payload from Kafka
     * @param topic the topic from which the message was received
     * @param sessionIdValue the session ID from custom Kafka header (if present)
     */
    @KafkaListener(topics = "#{'${ces.kafka.topics}'}")
    public void consume(
            @Payload FeedMessage feedMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = "sessionId", required = false) String sessionIdValue) {
        
        logger.debug("Received FeedMessage from topic: {} with message type: {}", 
                     topic, feedMessage.getHeader().getMessageType());
        
        // Extract session ID from header or derive from account_id in the payload
        SessionId sessionId = determineSessionId(sessionIdValue, feedMessage);
            
        EventMessage eventMessage = new EventMessage(sessionId, feedMessage, topic);
        deliverMessageUseCase.deliver(eventMessage);
    }
    
    /**
     * Determines the session ID from header or from the FeedMessage payload.
     * Many message types contain an account_id that can be used as session identifier.
     */
    private SessionId determineSessionId(String sessionIdValue, FeedMessage feedMessage) {
        if (sessionIdValue != null) {
            return SessionId.of(sessionIdValue);
        }
        
        // Try to extract account_id from various payload types
        try {
            return switch (feedMessage.getPayloadCase()) {
                case TRANS_PAYLOAD -> SessionId.of(String.valueOf(feedMessage.getTransPayload().getAccountId()));
                case LOGIN_PAYLOAD -> SessionId.of(String.valueOf(feedMessage.getLoginPayload().getAccountId()));
                case LOGOUT_PAYLOAD -> SessionId.of(String.valueOf(feedMessage.getLogoutPayload().getAccountId()));
                case REGISTRATION_PAYLOAD ->
                        SessionId.of(String.valueOf(feedMessage.getRegistrationPayload().getAccountId()));
                case ACCOUNT_CREATION_PAYLOAD ->
                        SessionId.of(String.valueOf(feedMessage.getAccountCreationPayload().getAccountId()));
                case PAYMENT_TRANS_PAYLOAD ->
                        SessionId.of(String.valueOf(feedMessage.getPaymentTransPayload().getAccountId()));
                case UPDATE_ACCOUNT_PAYLOAD ->
                        SessionId.of(String.valueOf(feedMessage.getUpdateAccountPayload().getAccountId()));
                case EXTEND_SESSION_PAYLOAD ->
                        SessionId.of(String.valueOf(feedMessage.getExtendSessionPayload().getAccountId()));
                default -> {
                    logger.warn("Unknown payload type: {}, generating random session ID", feedMessage.getPayloadCase());
                    yield SessionId.generate();
                }
            };
        } catch (Exception e) {
            logger.warn("Error extracting account_id from FeedMessage, generating random session ID", e);
            return SessionId.generate();
        }
    }
}
