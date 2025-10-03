# Protobuf Integration for Kafka Events

## Overview

The Customer Event Stream (CES) application now uses Protocol Buffers (Protobuf) for Kafka message serialization/deserialization. This integration is based on the **OPS-Events-schema** defined in `OPS-Events-schema/feed_message.proto`.

## Schema Details

### FeedMessage Structure

The main message type is `FeedMessage` from package `com.lnw.expressway.messages.v1`, which contains:

- **Header**: Metadata about the message including:
  - `timestamp`: Message creation time
  - `message_type`: Enum indicating payload type (e.g., WalletTransaction, Login, Logout, etc.)
  - `identifier`: Unique message identification with UUID, sequence key, and sequence ID
  - `system_ref`: Reference to the source system (Product, System, Tenant)

- **Payload**: One of 20+ different payload types:
  - `TransPayload`: Player wallet transactions
  - `LoginPayload`: Player login events
  - `LogoutPayload`: Player logout events
  - `RegistrationPayload`: Account validation
  - `AccountCreationPayload`: New account creation
  - `PaymentTransPayload`: Payment provider transactions
  - `UpdateAccountPayload`: Account property updates
  - `ExtendSessionPayload`: Session extension
  - And many more...

## Implementation Components

### 1. Maven Configuration (pom.xml)

Added dependencies and plugin configuration:

```xml
<properties>
    <protobuf.version>3.25.3</protobuf.version>
    <protobuf-maven-plugin.version>0.6.1</protobuf-maven-plugin.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
    </dependency>
</dependencies>

<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>${protobuf-maven-plugin.version}</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <protoSourceRoot>${project.basedir}/OPS-Events-schema</protoSourceRoot>
                <outputDirectory>${project.build.directory}/generated-sources/protobuf/java</outputDirectory>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2. Custom Kafka Deserializer

**Class**: `com.ces.infrastructure.kafka.FeedMessageDeserializer`

Implements `org.apache.kafka.common.serialization.Deserializer<FeedMessage>` to convert byte arrays from Kafka into `FeedMessage` objects.

Key features:
- Handles null data gracefully
- Logs deserialization errors
- Throws `SerializationException` on failure

### 3. Domain Model Updates

**EventMessage** (`com.ces.domain.model.EventMessage`)

Updated to work with Protobuf:
- Changed field from `String data` to `FeedMessage feedMessage`
- Updated constructors to accept `FeedMessage` instead of `String`
- Added `getData()` method to convert `FeedMessage` to string format for delivery

### 4. Kafka Consumer Adapter

**KafkaMessageConsumerAdapter** (`com.ces.infrastructure.adapter.KafkaMessageConsumerAdapter`)

Enhanced to:
- Consume `FeedMessage` directly from Kafka
- Extract `account_id` from various payload types to determine session ID
- Support multiple message types (Trans, Login, Logout, Registration, etc.)
- Fall back to generating random session ID for unknown payload types or missing headers

Session ID extraction logic:
```java
switch (feedMessage.getPayloadCase()) {
    case TRANS_PAYLOAD:
        return SessionId.of(String.valueOf(feedMessage.getTransPayload().getAccountId()));
    case LOGIN_PAYLOAD:
        return SessionId.of(String.valueOf(feedMessage.getLoginPayload().getAccountId()));
    // ... and more payload types
}
```

### 5. Configuration Files

All environment configurations updated to use `FeedMessageDeserializer`:

**application.yml** (default):
```yaml
spring:
  kafka:
    consumer:
      value-deserializer: com.ces.infrastructure.kafka.FeedMessageDeserializer
```

**application-dev.yml**, **application-prod.yml**, **application-test.yml**: Similar configuration applied.

## Proto File Configuration

The `feed_message.proto` file includes important options for Java code generation:

```protobuf
option java_outer_classname = "FeedMessageProto";
```

**Key Points:**
- `java_outer_classname = "FeedMessageProto"`: Ensures the generated outer class name matches Java imports
  - Without this option, protoc would generate `FeedMessageOuterClass` by default
  - With this option, the generated class is `com.lnw.expressway.messages.v1.FeedMessageProto`
- Uses standard Protobuf runtime (`protobuf-java`) which generates `GeneratedMessageV3` classes
  - Provides full reflection and descriptor support
  - Compatible with all Protobuf features

## Building the Project

### Option 1: Using Docker Build Scripts (Recommended for environments without Maven)

Quick compilation (skips tests):
```bash
./scripts/compile.sh
```

Full build with tests:
```bash
./scripts/build.sh
```

See `scripts/README.md` for detailed documentation.

### Option 2: Using Maven Directly

Generate Protobuf Classes:
```bash
mvn clean compile
```

This will generate classes in `target/generated-sources/protobuf/java/` from the schema in `OPS-Events-schema/feed_message.proto`.

Run Tests:
```bash
mvn test
```

Full Build with Integration Tests:
```bash
mvn clean verify
```

### Option 3: Using IntelliJ IDEA

1. Open Maven tool window: `View → Tool Windows → Maven`
2. Click "Reload All Maven Projects" (circular arrows icon)
3. Expand `Lifecycle` and double-click `compile` or `verify`

Or use the menu: `Build → Build Project`

## Usage Example

### Producing Messages to Kafka

When producing messages to Kafka topics, ensure they are serialized as Protobuf `FeedMessage`:

```java
FeedMessage message = FeedMessage.newBuilder()
    .setHeader(Header.newBuilder()
        .setTimestamp(Timestamp.newBuilder()
            .setSeconds(Instant.now().getEpochSecond())
            .build())
        .setMessageType(Header.MessageType.Login)
        .build())
    .setLoginPayload(LoginPayload.newBuilder()
        .setAccountId(123456789)
        .setLoginId(2025010301300000000L)
        .build())
    .build();

byte[] serialized = message.toByteArray();
// Send to Kafka topic
```

### Consuming Messages

The application automatically consumes and deserializes `FeedMessage` from configured topics. The `KafkaMessageConsumerAdapter` handles:
1. Deserialization from bytes to `FeedMessage`
2. Session ID extraction from headers or payload
3. Wrapping in domain `EventMessage`
4. Delivery through the use case layer

## Message Types Reference

| Message Type | Payload Type | Description |
|-------------|--------------|-------------|
| WalletTransaction (101) | TransPayload | Player wallet transactions |
| Login (102) | LoginPayload | Player login events |
| PaymentTransaction (103) | PaymentTransPayload | Payment provider transactions |
| PropertyAudit (104) | PropertyAuditPayload | Player property changes (deprecated) |
| Registration (105) | RegistrationPayload | Account validation |
| AccountCreation (106) | AccountCreationPayload | New account creation |
| GamingLimit (107) | GamingLimitPayload | Gaming limit changes |
| GamingLimitAudit (108) | GamingLimitAuditPayload | Gaming limit audit |
| GamingLimitHit (109) | GamingLimitHitPayload | Gaming limit reached |
| Blocklist (110) | BlocklistPayload | Blocklist changes (deprecated) |
| BlocklistLog (111) | BlocklistLogPayload | Blocklist audit |
| LoginLimitSetting (112) | LoginLimitSettingPayload | Login limit changes |
| LoginLimitHit (113) | LoginLimitHitPayload | Login limit reached |
| WalletLimitHit (114) | WalletLimitHitPayload | Wallet limit reached |
| WalletLimitAudit (115) | WalletLimitAuditPayload | Wallet limit changes |
| AccountRestrictionReasonAudit (116) | AccountRestrictionReasonAuditPayload | Account restrictions |
| RealityCheck (117) | RealityCheckPayload | Reality check notifications |
| UpdateAccount (118) | UpdateAccountPayload | Account property updates |
| Logout (119) | LogoutPayload | Player logout events |
| ExtendSession (120) | ExtendSessionPayload | Session extension events |

## Common Fields

### Account ID
Most message payloads contain an `account_id` field representing the 9-digit player ID. This is used to route messages to the correct session.

### Currency
Monetary amounts use 3-character ISO-4217 currency codes (e.g., EUR, USD, GBP).

### Unique IDs
Numeric IDs like `trans_id`, `login_id`, and `trace_id` follow a timestamp-like format (e.g., 2025010301300000000).

## Troubleshooting

### Compilation Issues

If you see "Cannot resolve symbol" errors:
1. Run `mvn clean compile` to generate Protobuf classes
2. Refresh your IDE project structure
3. Ensure `target/generated-sources/protobuf/java` is marked as source folder

### Deserialization Errors

Check logs for:
- Protobuf version mismatches between producer and consumer
- Corrupt or incomplete message data
- Schema version incompatibilities

### Session Routing Issues

If messages aren't reaching the correct sessions:
1. Verify `sessionId` header is present in Kafka messages
2. Check that `account_id` extraction logic covers your message type
3. Review logs for session ID determination warnings

## Future Improvements

- Add JSON serialization support for FeedMessage (using `com.google.protobuf.util.JsonFormat`)
- Implement message filtering based on message type
- Add metrics for different payload types
- Support for custom payload routing strategies
- Schema registry integration for version management

## References

- Protobuf Official Documentation: https://protobuf.dev/
- Spring Kafka Documentation: https://docs.spring.io/spring-kafka/reference/
- OPS-Events-schema: `OPS-Events-schema/feed_message.proto`
