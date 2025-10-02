package com.ces.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for Kafka consumer settings.
 * Binds to the 'ces.kafka' prefix in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "ces.kafka")
public class KafkaConsumerProperties {
    
    private List<String> topics;

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }
}
