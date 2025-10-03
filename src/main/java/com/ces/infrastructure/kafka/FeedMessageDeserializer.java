package com.ces.infrastructure.kafka;

import com.lnw.expressway.messages.v1.FeedMessageProto.FeedMessage;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Kafka deserializer for Protobuf FeedMessage.
 * Converts byte arrays from Kafka messages into FeedMessage objects.
 */
public class FeedMessageDeserializer implements Deserializer<FeedMessage> {

    private static final Logger logger = LoggerFactory.getLogger(FeedMessageDeserializer.class);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No specific configuration needed
    }

    @Override
    public FeedMessage deserialize(String topic, byte[] data) {
        if (data == null) {
            logger.warn("Received null data from topic: {}", topic);
            return null;
        }

        try {
            return FeedMessage.parseFrom(data);
        } catch (Exception e) {
            logger.error("Failed to deserialize FeedMessage from topic: {}", topic, e);
            throw new SerializationException("Error deserializing Protobuf FeedMessage", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
