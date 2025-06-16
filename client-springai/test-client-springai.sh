#!/bin/bash

# Test script for Spring AI MCP Client

set -e

echo "=== Spring AI MCP Client Test Runner ==="
echo

# Check if server JAR exists
SERVER_JAR="../build/libs/OsqueryMcpServer-1.0.jar"
if [[ ! -f "$SERVER_JAR" ]]; then
    echo "❌ Server JAR not found at $SERVER_JAR"
    echo "Please run './gradlew bootJar' from the project root first"
    exit 1
fi

echo "✅ Server JAR found at $SERVER_JAR"
echo

# Build the client
echo "🔨 Building Spring AI client..."
./gradlew build

echo
echo "🧪 Running unit tests..."
./gradlew test --tests QueryMappingTest

echo
echo "✅ All tests completed successfully!"
echo
echo "📊 Test Reports:"
echo "  - Unit tests: build/reports/tests/test/index.html"