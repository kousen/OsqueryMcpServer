#!/usr/bin/env bash
# Measure MCP server native binary startup time (cold start to first response)
# Run 2-3 times — first run may be slow due to macOS file system caching

time (echo '{"jsonrpc":"2.0","method":"initialize","id":1,"params":{"capabilities":{}}}' | ./build/native/nativeCompile/OsqueryMcpServer 2>/dev/null | head -1)
