package com.ces;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for the Customer Event Stream (CES) system.
 * 
 * This application consumes messages from Kafka topics and delivers them to
 * registered clients via WebSocket connections using session-based routing.
 */
@SpringBootApplication
@EnableKafka
public class CustomerEventStreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerEventStreamApplication.class, args);
    }
}
