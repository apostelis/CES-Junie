# Compilation Error Fix

## Issue

```
/Users/a.palogos/IdeaProjects/CES-Junie/src/main/java/com/ces/domain/model/EventMessage.java:3:55
java: package com.lnw.expressway.messages.v1.FeedMessageProto does not exist
```

## Root Cause

The compilation error occurred because:

1. **Missing Java Outer Class Name**: The `feed_message.proto` file didn't specify `java_outer_classname`, causing protoc to generate `FeedMessageOuterClass` by default instead of the expected `FeedMessageProto`
2. **Wrong Protobuf Dependency**: The pom.xml used `protobuf-java` instead of `protobuf-javalite`, which is required for the `LITE_RUNTIME` optimization specified in the proto file
3. **Classes Not Generated**: Protobuf Java classes weren't generated yet from the schema file

## Solution Applied

### 1. Fixed Proto File (OPS-Events-schema/feed_message.proto)

Added the `java_outer_classname` option to ensure generated classes match the imports:

```protobuf
syntax = "proto3";

package com.lnw.expressway.messages.v1;
import "google/protobuf/timestamp.proto";

option optimize_for = LITE_RUNTIME;
option java_outer_classname = "FeedMessageProto";  // <- ADDED
```

**Impact**: Now protoc generates `FeedMessageProto.java` instead of `FeedMessageOuterClass.java`, matching the imports in:
- `EventMessage.java`
- `FeedMessageDeserializer.java`
- `KafkaMessageConsumerAdapter.java`

### 2. Fixed Protobuf Dependency (pom.xml)

Replaced `protobuf-java` with `protobuf-javalite`:

```xml
<!-- BEFORE -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>${protobuf.version}</version>
</dependency>

<!-- AFTER -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-javalite</artifactId>
    <version>${protobuf.version}</version>
</dependency>
```

**Why**: The proto file uses `option optimize_for = LITE_RUNTIME`, which generates code that extends `GeneratedMessageLite`. This requires the `protobuf-javalite` runtime library, not `protobuf-java`.

### 3. Added Build Scripts (scripts/)

Created Docker-based build scripts for environments without Maven:

- **scripts/compile.sh**: Quick compilation (skips tests)
- **scripts/build.sh**: Full build with tests
- **scripts/README.md**: Documentation

These scripts use the official `maven:3.9-eclipse-temurin-21` Docker image to ensure reproducible builds.

## How to Build

### Option 1: Using Build Scripts (if Maven not installed)

```bash
# Quick compilation
./scripts/compile.sh

# Full build with tests
./scripts/build.sh
```

### Option 2: Using Maven

```bash
# Generate Protobuf classes and compile
mvn clean compile

# Run tests
mvn test

# Full build
mvn clean verify
```

### Option 3: Using IntelliJ IDEA

1. Open Maven tool window: `View → Tool Windows → Maven`
2. Click "Reload All Maven Projects" (circular arrows)
3. Expand `Lifecycle` and double-click `compile` or `verify`

Or: `Build → Build Project`

## Verification

After building, verify the generated classes exist:

```bash
ls -la target/generated-sources/protobuf/java/com/lnw/expressway/messages/v1/
```

You should see:
```
FeedMessageProto.java
```

This file contains:
- The `FeedMessage` class
- All payload classes (TransPayload, LoginPayload, etc.)
- Header and related message types

## Files Modified

1. `OPS-Events-schema/feed_message.proto` - Added `java_outer_classname` option
2. `pom.xml` - Replaced `protobuf-java` with `protobuf-javalite`
3. `docs/protobuf-integration.md` - Updated with proto options and build instructions

## Files Created

1. `scripts/compile.sh` - Quick compilation script
2. `scripts/build.sh` - Full build script
3. `scripts/README.md` - Build scripts documentation
4. `docs/COMPILATION-FIX.md` - This document

## Next Steps

1. Build the project using one of the methods above
2. Verify compilation succeeds
3. Run tests to ensure everything works: `mvn test` or `./scripts/build.sh`
4. Commit the changes with message: `fix: resolve Protobuf class generation issues`

## References

- Protobuf integration guide: `docs/protobuf-integration.md`
- Build scripts documentation: `scripts/README.md`
- Proto schema: `OPS-Events-schema/feed_message.proto`
- Proto schema documentation: `OPS-Events-schema/README.md`
