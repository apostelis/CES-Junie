# OPS Events Schema

## Overview

This directory contains the Protocol Buffer (Protobuf) schema definition for events from the OPS (Online Platform System) that are published to Kafka and consumed by the Customer Event Stream (CES) application.

## Schema File

**File**: `feed_message.proto`

**Package**: `com.lnw.expressway.messages.v1`

**Syntax**: proto3

**Java Outer Class**: FeedMessageProto

## Main Message: FeedMessage

The `FeedMessage` is the wrapper message that contains all event types. It consists of:

1. **Header**: Common metadata for all messages
2. **Payload**: One of 20+ specific payload types (using protobuf oneof)

## Message Structure

```
FeedMessage
├── Header
│   ├── timestamp (google.protobuf.Timestamp)
│   ├── message_type (MessageType enum)
│   ├── identifier (Identifier)
│   │   ├── key (SequencingKey enum)
│   │   ├── sequence_id (uint64)
│   │   ├── uuid (string)
│   │   └── reference (string, optional)
│   └── system_ref (SystemRef)
│       ├── product (Product enum: OPS, OGS)
│       ├── system (System enum: Account, Wallet, Payments)
│       └── tenant (string)
└── Payload (oneof - one of the following)
    ├── TransPayload (wallet transactions)
    ├── LoginPayload (login events)
    ├── LogoutPayload (logout events)
    ├── PaymentTransPayload (payment transactions)
    ├── RegistrationPayload (account validation)
    ├── AccountCreationPayload (new accounts)
    ├── UpdateAccountPayload (account updates)
    ├── ExtendSessionPayload (session extensions)
    └── ... (12 more payload types)
```

## Payload Types

| Index | Message Type | Payload | Description |
|-------|-------------|---------|-------------|
| 101 | WalletTransaction | TransPayload | Player wallet transactions (deposits, withdrawals, bets, wins) |
| 102 | Login | LoginPayload | Successful login or failed login attempt |
| 103 | PaymentTransaction | PaymentTransPayload | Payment provider transactions |
| 104 | PropertyAudit | PropertyAuditPayload | Player property changes (deprecated - use UpdateAccount) |
| 105 | Registration | RegistrationPayload | Player account validation completion |
| 106 | AccountCreation | AccountCreationPayload | New player account creation |
| 107 | GamingLimit | GamingLimitPayload | Gaming limit set or changed |
| 108 | GamingLimitAudit | GamingLimitAuditPayload | Gaming limits audit (deprecated) |
| 109 | GamingLimitHit | GamingLimitHitPayload | Gaming limit reached |
| 110 | Blocklist | BlocklistPayload | Blocklist changes (deprecated - use BlocklistLog) |
| 111 | BlocklistLog | BlocklistLogPayload | Blocklist audit with full info |
| 112 | LoginLimitSetting | LoginLimitSettingPayload | Login limit set or changed |
| 113 | LoginLimitHit | LoginLimitHitPayload | Login limit reached |
| 114 | WalletLimitHit | WalletLimitHitPayload | Wallet limit reached |
| 115 | WalletLimitAudit | WalletLimitAuditPayload | Wallet limit set or changed |
| 116 | AccountRestrictionReasonAudit | AccountRestrictionReasonAuditPayload | Account restriction flag raised or removed |
| 117 | RealityCheck | RealityCheckPayload | Reality check notification sent to player |
| 118 | UpdateAccount | UpdateAccountPayload | Multiple player properties changed |
| 119 | Logout | LogoutPayload | Player logout (explicit or abandoned session) |
| 120 | ExtendSession | ExtendSessionPayload | Player session expiration extended |

## Common Fields

### account_id
- Type: `uint32`
- Description: 9-digit player account ID
- Present in most payload types
- Used to route messages to the correct player session

### currency
- Type: `string`
- Format: ISO-4217 3-character code (e.g., EUR, USD, GBP)
- Used in all monetary amount fields

### Unique IDs
Numeric IDs like `trans_id`, `login_id`, `trace_id`:
- Type: `uint64`
- Format: Timestamp-like (e.g., 2025010301300000000)
- Globally unique within their domain

## Key Payload Details

### TransPayload (Wallet Transactions)
Most important fields:
- `trans_id`: Transaction unique ID
- `account_id`: Player ID
- `trans_type`: Type of transaction (DEPOSIT, WITHDRAW, BET, RESULT, etc.)
- `delta_*`: Balance changes (cash, bonus, goods, etc.)
- `bal_*`: Balances after transaction (optional)
- `currency`: Transaction currency
- `game`, `config`, `round`: Game information
- `login_id`: Associated login session (optional)

### LoginPayload
- `login_id`: Unique login session ID
- `account_id`: Player ID
- `login_time`: Login timestamp
- `logout_time`: Initial session expiration time
- `ip`, `indirect_ip`: IP addresses
- `channel`: Login channel (web, mobile, etc.)
- `is_failed_login`: True for failed login attempts

### LogoutPayload
- `login_id`: Login session ID being closed
- `account_id`: Player ID
- `login_time`: When session started
- `logout_time`: When session ended
- `channel`: Logout channel

### PaymentTransPayload
- `id`: Payment transaction ID
- `account_id`: Player ID
- `provider`, `method`: Payment provider and method details
- `trans_type`: DEPOSIT, WITHDRAW, REFUND
- `status`: Transaction status
- `amount`, `fee`, `currency`: Financial details

## Dependencies

This schema requires:
- `google/protobuf/timestamp.proto` - for timestamp fields

## Compilation

To generate Java classes from this schema:

```bash
mvn clean compile
```

Java classes will be generated to: `target/generated-sources/protobuf/java/com/lnw/expressway/messages/v1/`

## Usage in CES Application

The CES application:
1. Consumes binary Protobuf messages from Kafka topics
2. Deserializes them using `FeedMessageDeserializer`
3. Extracts `account_id` to determine target session
4. Wraps in domain `EventMessage` object
5. Delivers to connected WebSocket clients

See `docs/protobuf-integration.md` for full integration details.

## Schema Version

**Current Version**: Based on OPS-Events v1

**Last Updated**: 2025-10-02

## Notes

- Some fields and message types are marked as deprecated but maintained for backwards compatibility
- The schema uses standard Protobuf runtime with full reflection and descriptor support
- Optional fields use the `optional` keyword (proto3 syntax)
- Map types are used for flexible property storage (e.g., `trans_properties`, `account_properties`)

## Support

For questions about the schema or integration:
- See the main project documentation in `docs/`
- Check `docs/protobuf-integration.md` for implementation details
- Review `docs/kafka-configuration-guide.md` for Kafka setup
