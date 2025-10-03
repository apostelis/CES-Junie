#!/usr/bin/env bash
set -euo pipefail

# Build script using Maven Docker image
# Requires Docker to be installed and running
# This script ensures reproducible builds across environments

echo "Building project with Maven Docker image..."
echo "This will:"
echo "  1. Generate Java classes from Protobuf schema"
echo "  2. Compile the project"
echo "  3. Run tests"
echo ""

docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn clean verify

echo ""
echo "Build completed successfully!"
echo "Generated Protobuf classes can be found in: target/generated-sources/protobuf/java"
