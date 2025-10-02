package com.ces.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KafkaConsumerProperties.
 */
class KafkaConsumerPropertiesTest {

    @Test
    void shouldSetAndGetTopics() {
        // given
        KafkaConsumerProperties properties = new KafkaConsumerProperties();
        List<String> expectedTopics = Arrays.asList("topic1", "topic2", "topic3");

        // when
        properties.setTopics(expectedTopics);

        // then
        assertEquals(expectedTopics, properties.getTopics());
    }

    @Test
    void shouldHandleEmptyTopicsList() {
        // given
        KafkaConsumerProperties properties = new KafkaConsumerProperties();
        List<String> emptyTopics = Collections.emptyList();

        // when
        properties.setTopics(emptyTopics);

        // then
        assertNotNull(properties.getTopics());
        assertTrue(properties.getTopics().isEmpty());
    }

    @Test
    void shouldHandleSingleTopic() {
        // given
        KafkaConsumerProperties properties = new KafkaConsumerProperties();
        List<String> singleTopic = Collections.singletonList("single-topic");

        // when
        properties.setTopics(singleTopic);

        // then
        assertEquals(1, properties.getTopics().size());
        assertEquals("single-topic", properties.getTopics().getFirst());
    }

    @Test
    void shouldReturnNullWhenTopicsNotSet() {
        // given
        KafkaConsumerProperties properties = new KafkaConsumerProperties();

        // when
        List<String> topics = properties.getTopics();

        // then
        assertNull(topics);
    }

    @Test
    void shouldHandleTopicsWithSpecialCharacters() {
        // given
        KafkaConsumerProperties properties = new KafkaConsumerProperties();
        List<String> specialTopics = Arrays.asList(
            "customer.events",
            "order_created",
            "payment-processed",
            "topic_123"
        );

        // when
        properties.setTopics(specialTopics);

        // then
        assertEquals(4, properties.getTopics().size());
        assertEquals("customer.events", properties.getTopics().get(0));
        assertEquals("order_created", properties.getTopics().get(1));
        assertEquals("payment-processed", properties.getTopics().get(2));
        assertEquals("topic_123", properties.getTopics().get(3));
    }
}
