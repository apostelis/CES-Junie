#!/usr/bin/env bash
set -euo pipefail

# Compile script using Maven Docker image (skips tests for faster execution)
# Requires Docker to be installed and running
# Use this for quick Protobuf class generation and compilation

echo "Compiling project with Maven Docker image (skipping tests)..."
echo "This will:"
echo "  1. Generate Java classes from Protobuf schema"
echo "  2. Compile the project"
echo ""

docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn clean compile -DskipTests

echo ""
echo "Compilation completed successfully!"
echo "Generated Protobuf classes can be found in: target/generated-sources/protobuf/java"
