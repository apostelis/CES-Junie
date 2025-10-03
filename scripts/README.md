# Build Scripts

This directory contains scripts for building the project in environments where Maven is not installed locally.

## Prerequisites

- Docker must be installed and running
- These scripts use the official Maven Docker image (`maven:3.9-eclipse-temurin-21`)

## Available Scripts

### compile.sh

Quick compilation script that generates Protobuf classes and compiles the project (skips tests).

```bash
./scripts/compile.sh
```

**Use this when:**
- You need to quickly generate Protobuf Java classes
- You want to verify that the code compiles
- You're iterating on code changes

### build.sh

Full build script that generates Protobuf classes, compiles, and runs all tests.

```bash
./scripts/build.sh
```

**Use this when:**
- You want to run the complete build pipeline
- You need to verify tests pass
- You're preparing for a commit or deployment

## Alternative: Using IntelliJ IDEA

If you're using IntelliJ IDEA, you can build without these scripts:

1. Open the Maven tool window: `View → Tool Windows → Maven`
2. Click "Reload All Maven Projects" (circular arrows icon)
3. Expand `Lifecycle` and double-click `compile` or `verify`

Or use the menu: `Build → Build Project`

## Generated Files

After running either script, Protobuf-generated Java classes will be available at:
```
target/generated-sources/protobuf/java/
```

The generated package structure:
```
com/lnw/expressway/messages/v1/
  └── FeedMessageProto.java (contains FeedMessage and all payload classes)
```

## Troubleshooting

**"Cannot connect to Docker daemon"**
- Ensure Docker Desktop is running
- Try: `docker ps` to verify Docker is accessible

**"Permission denied"**
- Make scripts executable: `chmod +x scripts/*.sh`

**"Volume mounting fails"**
- Ensure you're running from the project root directory
- The scripts use `$PWD` to mount the current directory
