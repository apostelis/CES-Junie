# Kafka Configuration Guide - YAML Approach

## Overview

This guide documents the YAML-based configuration approach for Kafka topic consumption in the Customer Event Stream (CES) application, following Spring Boot 3.x conventions and hexagonal architecture principles.

## Configuration Files

### Main Configuration (application.yml)

Located at: `src/main/resources/application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ces-consumer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      enable-auto-commit: true
      properties:
        spring.json.trusted.packages: "*"

ces:
  kafka:
    topics:
      - customer-events
      - order-events
      - notification-events
  websocket:
    base-url: ws://localhost:8080/ws
```

### Environment-Specific Configurations

#### Development (application-dev.yml)
- Uses localhost Kafka broker
- Dev-prefixed topic names
- Located at: `src/main/resources/application-dev.yml`

#### Production (application-prod.yml)
- Multiple Kafka brokers for high availability
- Production topic names
- Secure WebSocket URL (wss://)
- Located at: `src/main/resources/application-prod.yml`

#### Test (application-test.yml)
- Embedded Kafka broker for testing
- Test-specific topics
- Located at: `src/test/resources/application-test.yml`

## Architecture Components

### 1. Configuration Properties Class

**File:** `src/main/java/com/ces/infrastructure/config/KafkaConsumerProperties.java`

Binds YAML properties under the `ces.kafka` prefix to a strongly-typed Java class.

```java
@Configuration
@ConfigurationProperties(prefix = "ces.kafka")
public class KafkaConsumerProperties {
    private List<String> topics;
    // getters and setters
}
```

### 2. Kafka Consumer Adapter

**File:** `src/main/java/com/ces/infrastructure/adapter/KafkaMessageConsumerAdapter.java`

Implements the infrastructure layer adapter that:
- Listens to configured Kafka topics using `@KafkaListener`
- Extracts message data and metadata (topic, session ID)
- Delivers messages through the domain layer via `DeliverMessageUseCase`

Key features:
- Uses SpEL expression `#{'${ces.kafka.topics}'}` to read topics from configuration
- Extracts session ID from Kafka headers or generates a new one
- Maintains hexagonal architecture by using application ports

### 3. Main Application Class

**File:** `src/main/java/com/ces/CustomerEventStreamApplication.java`

Spring Boot application entry point with:
- `@SpringBootApplication` for auto-configuration
- `@EnableKafka` to enable Kafka support

## How to Use

### Running with Different Profiles

**Development:**
```bash
java -jar customer-event-stream.jar --spring.profiles.active=dev
```

**Production:**
```bash
java -jar customer-event-stream.jar --spring.profiles.active=prod
```

**Testing:**
Tests automatically use the test profile when configured with `@ActiveProfiles("test")`

### Configuring Topics

To add or modify topics, edit the appropriate YAML file:

```yaml
ces:
  kafka:
    topics:
      - new-topic-name
      - another-topic
```

### Overriding Configuration via Environment Variables

You can override any property using environment variables:

```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092
export CES_KAFKA_TOPICS=topic1,topic2,topic3
```

### Overriding Configuration via Command Line

```bash
java -jar customer-event-stream.jar \
  --spring.kafka.bootstrap-servers=kafka:9092 \
  --ces.kafka.topics=topic1,topic2
```

## Architecture Compliance

### Hexagonal Architecture

The implementation follows hexagonal (ports and adapters) architecture:

**Domain Layer** (`com.ces.domain`)
- Pure domain models (Session, SessionId, EventMessage)
- No framework dependencies
- Business logic encapsulated

**Application Layer** (`com.ces.application`)
- Port interfaces (DeliverMessageUseCase, RegisterSessionUseCase)
- Service implementations
- Orchestrates domain operations

**Infrastructure Layer** (`com.ces.infrastructure`)
- Kafka adapter (KafkaMessageConsumerAdapter)
- Configuration properties (KafkaConsumerProperties)
- Framework-specific implementations

### Benefits

1. **Framework Independence**: Domain logic is independent of Spring/Kafka
2. **Testability**: Easy to test with mocked ports
3. **Flexibility**: Infrastructure can be replaced without touching domain
4. **Maintainability**: Clear separation of concerns

## Dependencies

The following dependencies are required in `pom.xml`:

- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-websocket` - WebSocket support
- `spring-kafka` - Kafka integration
- `spring-boot-configuration-processor` - Configuration metadata generation

## Troubleshooting

### Topics Not Being Consumed

1. Check Kafka broker connectivity
2. Verify topic names match exactly
3. Check consumer group configuration
4. Review application logs for connection errors

### Configuration Not Loading

1. Ensure YAML syntax is correct (indentation matters)
2. Verify profile is active (`--spring.profiles.active=dev`)
3. Check for typos in property names
4. Enable debug logging: `logging.level.org.springframework.boot.context.config=DEBUG`

### Session ID Issues

If session IDs are not being extracted from Kafka messages:
1. Ensure Kafka producer is setting the `sessionId` header
2. Alternatively, implement custom logic to extract session ID from message payload
3. The adapter currently falls back to generating a new session ID

## Future Enhancements

Potential improvements to consider:

1. **Dynamic Topic Subscription**: Allow runtime topic configuration changes
2. **Message Filtering**: Add routing rules based on message content
3. **Dead Letter Queue**: Handle failed message delivery
4. **Metrics**: Add Kafka consumer metrics (lag, throughput)
5. **Security**: Add SSL/SASL authentication for Kafka
6. **Message Validation**: Add schema validation (Avro/JSON Schema)

## Related Requirements

This implementation satisfies the following functional requirements from `docs/requirements/requirements.md`:

- **FR-001**: Kafka Topic Subscription
- **FR-002**: Message Processing
- **FR-015**: Kafka Configuration Settings

## Support

For questions or issues, refer to:
- Spring Kafka documentation: https://spring.io/projects/spring-kafka
- Project requirements: `docs/requirements/requirements.md`
- Architecture diagrams: (to be added)
