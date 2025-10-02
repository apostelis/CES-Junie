package com.ces.infrastructure.adapter;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.domain.model.EventMessage;
import com.ces.domain.model.SessionId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter that listens to configured topics and delivers messages
 * to registered sessions through the domain layer.
 * This adapter bridges the Kafka infrastructure with the application's hexagonal architecture.
 */
@Component
public class KafkaMessageConsumerAdapter {
    
    private final DeliverMessageUseCase deliverMessageUseCase;
    
    public KafkaMessageConsumerAdapter(DeliverMessageUseCase deliverMessageUseCase) {
        this.deliverMessageUseCase = deliverMessageUseCase;
    }
    
    /**
     * Consumes messages from configured Kafka topics.
     * The topics are configured in application.yml under ces.kafka.topics.
     * 
     * @param message the message payload from Kafka
     * @param topic the topic from which the message was received
     * @param sessionIdValue the session ID from custom Kafka header (if present)
     */
    @KafkaListener(topics = "#{'${ces.kafka.topics}'}")
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = "sessionId", required = false) String sessionIdValue) {
        
        // If session ID is not in headers, we might need to extract it from the message
        // For now, we'll generate one or use a default approach
        SessionId sessionId = (sessionIdValue != null) 
            ? SessionId.of(sessionIdValue) 
            : SessionId.generate();
            
        EventMessage eventMessage = new EventMessage(sessionId, message, topic);
        deliverMessageUseCase.deliver(eventMessage);
    }
}
