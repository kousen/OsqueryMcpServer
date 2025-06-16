#!/bin/bash

echo "========================================="
echo "Spring AI MCP Client - Output Formatting Demo"
echo "========================================="
echo ""

# Navigate to client directory
cd client-springai

echo "1. Testing formatted output (default):"
echo "--------------------------------------"
../gradlew -q run --args="\"What's using my CPU?\"" 2>/dev/null | tail -n +20

echo ""
echo ""
echo "2. Testing raw JSON output:"
echo "--------------------------"
../gradlew -q run --args="--raw \"What's using my CPU?\"" 2>/dev/null | tail -n +20

echo ""
echo ""
echo "3. Testing memory usage (formatted):"
echo "-----------------------------------"
../gradlew -q run --args="\"What's using my memory?\"" 2>/dev/null | tail -n +20

echo ""
echo "Demo complete!"