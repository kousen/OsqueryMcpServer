# Spring AI MCP Client for Osquery

A simplified MCP client implementation using Spring AI's autoconfiguration capabilities that demonstrates the power of declarative MCP integration.

## Overview

This client demonstrates a **clean and simple** approach to creating an MCP client using Spring AI's autoconfiguration. It leverages Spring AI's MCP starter to automatically handle:

- ✅ **MCP protocol negotiation** - Automatic handshake and initialization
- ✅ **Server process management** - Automatic subprocess lifecycle management  
- ✅ **Tool discovery and registration** - Zero-code tool mapping via `SyncMcpToolCallbackProvider`
- ✅ **Error handling and timeouts** - Built-in resilience and timeout management
- ✅ **Connection management**—Automatic STDIO transport handling

## Architecture

- **Spring Boot Application**: Main application using Spring AI MCP client starter
- **Autoconfiguration**: Tools are automatically discovered via `SyncMcpToolCallbackProvider` injection
- **YAML Configuration**: Server connection details defined in `application.yml` 
- **Query Mapping**: Simple logic to map natural language queries to prefixed tool names
- **Zero Protocol Code**: No manual JSON-RPC, no manual process management

## Key Benefits over Manual Implementation

1. **50% Less Code**: ~150 lines vs ~250 lines in manual implementation
2. **Zero MCP Protocol Code**: Framework handles all JSON-RPC 2.0 protocol details
3. **Configuration-Driven**: YAML-based setup instead of hardcoded values
4. **Better Error Handling**: Built into Spring AI framework with proper timeouts
5. **Industry Standard**: Follows Spring Boot conventions and patterns
6. **Proven Performance**: Tools execute quickly (1–3 seconds) matching Claude Desktop performance

## Configuration

The client is configured via `application.yml`:

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: osquery-cli
        version: 1.0.0
        request-timeout: 30s
        type: SYNC
        toolcallback:
          enabled: true  # CRITICAL: Required for tool discovery
        stdio:
          connections:
            osquery-server:
              command: java
              args:
                - -Dspring.main.banner-mode=off
                - -Dlogging.pattern.console=
                - -jar
                - ../build/libs/OsqueryMcpServer-1.0.jar
```

### Key Configuration Notes

- **`toolcallback.enabled: true`** - Essential for automatic tool discovery
- **`type: SYNC`** - Use synchronous client for simple applications
- **`request-timeout: 30s`** - Prevents hanging on slow queries
- **STDIO transport** — Automatic subprocess management for the osquery server

## Usage

### Build and Run

```bash
# Build the server JAR (required dependency)
cd .. && ./gradlew bootJar

# Build the client
./gradlew build

# Run with a query (formatted output by default)
./gradlew run --args="\"What's using my CPU?\""

# Run with raw JSON output
./gradlew run --args="--raw \"What's using my CPU?\""

# Run in interactive mode  
./gradlew run --args="--interactive"
./gradlew run --args="-i"  # Short form

# Test system health
./gradlew run --args="\"system health\""

# Test SQL query
./gradlew run --args="\"SELECT name FROM system_info\""
```

### Output Formatting

The client now includes intelligent output formatting:

- **Formatted output (default)**: Data is displayed in readable tables or key-value pairs
- **Raw output**: Original JSON response from the server (use `--raw` flag)
- **Interactive toggle**: Type `raw` in interactive mode to toggle output format

#### Example: Formatted Table Output
```
+------------------------+-------+-------------+-------+
| name                   | pid   | cpu_percent | uid   |
+------------------------+-------+-------------+-------+
| WindowServer           | 320   | 15.2        | 88    |
| Chrome                 | 1234  | 12.5        | 501   |
| Slack                  | 5678  | 8.3         | 501   |
+------------------------+-------+-------------+-------+

Total: 3 rows
```

#### Example: Key-Value Output
```
computer_name  : MacBook Pro
host_name      : macbook.local
kernel_version : 23.5.0
osquery_version: 5.11.0
```

### Natural Language Queries

The client maps natural language queries to appropriate osquery tools:

- **CPU**: "What's using my CPU?" → `osquery_cli_osquery_server_getHighCpuProcesses`
- **Memory**: "What's using my memory?" → `osquery_cli_osquery_server_getHighMemoryProcesses` 
- **Network**: "Show network connections" → `osquery_cli_osquery_server_getNetworkConnections`
- **Temperature**: "Why is my fan running?" → `osquery_cli_osquery_server_getTemperatureInfo`
- **Health**: "System health" → `osquery_cli_osquery_server_getSystemHealthSummary`
- **Tables**: "List tables" → `osquery_cli_osquery_server_listOsqueryTables`
- **SQL**: "SELECT * FROM processes" → `osquery_cli_osquery_server_executeOsquery`

### Interactive Mode Commands

- `help` - Show available queries and commands
- `tools` - List all discovered MCP tools
- `raw` - Toggle between formatted and raw JSON output
- `exit` or `quit` - Exit the program

### Tool Naming Convention

Spring AI MCP automatically prefixes tool names with the client and server identifiers:
- **Format**: `{client_name}_{server_name}_{tool_name}`
- **Example**: `osquery_cli_osquery_server_getSystemHealthSummary`
- **Auto-discovery**: All tools are discovered automatically via `SyncMcpToolCallbackProvider`

## Testing

### Unit Tests

Test the query mapping logic:

```bash
./gradlew test --tests QueryMappingTest
```

### Integration Tests

Test the full MCP client integration:

```bash
./gradlew test --tests SpringAiMcpIntegrationTest
```

**Note**: Integration tests require the server JAR to be built first. These tests verify:
- MCP client autowiring and tool discovery
- Tool execution with real osquery data
- Proper handling of prefixed tool names
- Error handling for invalid queries

### Manual Testing

For quick verification that the MCP integration is working:

```bash
# Quick test to see tool discovery
./gradlew run

# Test a specific query
./gradlew run --args="\"system health\""
```

**Expected Output**:
- ✅ Found 11 tools with `osquery_cli_osquery_server_` prefix
- ✅ Successful tool execution with real system data
- ✅ Network connections, temperature, and system info displayed

### Test Coverage

The test suite includes:

1. **Query Mapping Tests** (`QueryMappingTest`):
   - CPU, memory, network, temperature, health queries
   - SQL query detection
   - Case insensitivity
   - Edge cases and complex queries
   - Default behavior for unknown queries

2. **Integration Tests** (`SpringAiMcpIntegrationTest`):
   - MCP client autowiring and tool discovery
   - Tool execution (simple tools and SQL queries)
   - Error handling for invalid queries
   - Tool definition validation

### Running All Tests

```bash
./test-client-springai.sh
```

## Dependencies

- **Spring Boot 4.0.1** — Application framework
- **Spring AI 2.0.0-M2** — MCP client starter with autoconfiguration
- **PicoCLI 4.7.5** - Command-line interface framework
- **Jackson 3** (`tools.jackson`) — JSON parsing for output formatting
- **JUnit 5 and AssertJ** — Testing framework

### Critical Dependency Notes

- **Spring AI BOM 2.0.0-M2** - Ensures compatible versions of all Spring AI components (requires Spring Milestones repo)
- **MCP Client Starter** - Provides `SyncMcpToolCallbackProvider` and auto-configuration
- **Java 25** - Required for Spring Boot 4.0.1
- **Jackson 3 JsonMapper** — Immutable builder pattern (`JsonMapper.builder().build()`), unchecked exceptions

## Comparison with Manual Implementation

| Aspect                 | Manual Implementation            | Spring AI Implementation                    |
|------------------------|----------------------------------|---------------------------------------------|
| **Code Size**          | ~250 lines across multiple files | ~150 lines in single file                   |
| **MCP Protocol**       | Manual JSON-RPC handling         | Auto-handled by framework                   |
| **Configuration**      | Hardcoded in Java                | YAML-based configuration                    |
| **Error Handling**     | Manual implementation            | Built into framework                        |
| **Process Management** | Manual subprocess handling       | Auto-managed by Spring AI                   |
| **Tool Discovery**     | Manual tool name mapping         | Automatic via `SyncMcpToolCallbackProvider` |
| **Dependencies**       | Minimal (Jackson, PicoCLI)       | Spring ecosystem                            |
| **Performance**        | Custom timeout handling          | Framework-managed timeouts                  |
| **Maintainability**    | High complexity                  | Low complexity                              |
| **Learning Curve**     | MCP protocol knowledge required  | Spring Boot patterns                        |
| **Reliability**        | Custom error handling            | Production-tested framework                 |

## Files

- `src/main/java/SpringAiOsqueryClientApplication.java` - Main application
- `src/main/resources/application.yml` - Configuration
- `src/test/java/QueryMappingTest.java` - Unit tests for query mapping
- `src/test/java/SpringAiMcpIntegrationTest.java` - Integration tests
- `test-client-springai.sh` - Test runner script
- `README.md` - This documentation