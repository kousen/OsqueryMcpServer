# Claude AI Assistant Instructions

This document provides context and instructions for AI assistants working on the Osquery MCP Server project.

## Project Purpose

The **Osquery MCP Server** is a Spring Boot application that acts as an intelligent bridge between AI models and the operating system. It translates natural language questions like "Why is my fan running so hot?" or "What's using all my memory?" into precise Osquery SQL queries, enabling AI assistants to diagnose system issues, monitor performance, and investigate security concerns.

The project now includes a **complete Spring AI MCP client implementation** that demonstrates how to communicate with the server through the Model Context Protocol using Spring AI's auto-configuration.

**Key Point**: This is NOT a production service exposed to untrusted users. It's designed for local use by AI assistants to help with system diagnostics through natural language interaction.

## Architecture

- **Spring Boot 3.5** with **Java 21** (uses Java 17+ features)
- **Model Context Protocol (MCP)** server using Spring AI's MCP starter
- **STDIO-based communication** for integration with Claude Desktop and other MCP tools
- **9 specialized diagnostic tools** exposed via `@Tool` annotations
- **ProcessBuilder** for robust process management with proper resource handling
- **Query timeouts**: 30 seconds for queries, 5 seconds for version checks
- **Execution time logging** for performance monitoring

## Available Tools

### Core Tools
- `executeOsquery(sql)` - Execute any Osquery SQL query
- `listOsqueryTables()` - List all available Osquery tables
- `getTableSchema(tableName)` - Get column information for any table

### Diagnostic Tools
- `getHighCpuProcesses()` - Find CPU-intensive processes
- `getHighMemoryProcesses()` - Find memory-intensive processes
- `getNetworkConnections()` - Show active network connections
- `getTemperatureInfo()` - System temperature and fan speeds (macOS)

### Helper Tools
- `getCommonQueries()` - Example queries for common scenarios
- `getSystemHealthSummary()` - Comprehensive overview of CPU, memory, disk, network, and temperature

## Project Structure

```
src/                                          # MCP Server
├── main/
│   ├── java/com/kousenit/osquerymcpserver/
│   │   ├── OsqueryMcpServerApplication.java  # Main Spring Boot app
│   │   └── OsqueryService.java               # Core service with @Tool methods
│   └── resources/
│       └── application.properties            # MCP server configuration
└── test/
    ├── java/com/kousenit/osquerymcpserver/
    │   └── OsqueryServiceTest.java           # Comprehensive test suite
    └── resources/
        └── application-test.properties       # Test-specific logging config

client-springai/                              # Spring AI MCP Client
├── src/
│   ├── main/java/com/kousenit/osqueryclient/springai/
│   │   └── SpringAiOsqueryClientApplication.java  # Main Spring Boot app using Spring AI
│   ├── resources/
│   │   └── application.yml                   # Spring AI MCP configuration
│   └── test/java/com/kousenit/osqueryclient/springai/
│       └── QueryMappingTest.java             # Unit tests for query mapping
├── build.gradle.kts                          # Spring AI client build config
├── README.md                                 # Spring AI client documentation
└── test-client-springai.sh                  # Test script for Spring AI client

docs/
└── plan.md                                   # Development roadmap
```

## Development Guidelines

### Adding New Tools
When adding new `@Tool` methods to `OsqueryService`:
1. Use descriptive `@Tool(description = "...")` annotations
2. Include example use cases in the description
3. Add debug logging with `logger.debug()`
4. Create corresponding tests in `OsqueryServiceTest`

### Testing
- Tests use `@ActiveProfiles("test")` for debug logging
- Test output shows actual query results for verification
- Run server tests with: `./gradlew test --tests OsqueryServiceTest`
- Run client tests with: `cd client && ../gradlew test`

### Logging
- Production: Console logging disabled for STDIO compatibility
- Testing: Debug logs enabled and written to `test-osquery-mcp.log`
- Uncomment debug lines in `application.properties` for production debugging
- Query execution times are logged at debug level for performance monitoring
- Test log files are excluded from version control via .gitignore

### Security Considerations
- This tool executes system commands with user privileges
- Designed for trusted AI assistant use, not public exposure
- Osquery is read-only by design, but be mindful of information disclosure

### Implementation Details
- **ProcessBuilder** is used instead of Runtime.exec() for better resource management
- **Text blocks** (Java 15+) for multi-line SQL queries improve readability
- **`.formatted()`** method (Java 15+) replaces String.format() for cleaner code
- **Proper timeout handling** prevents hanging processes (30s for queries, 5s for version check)
- **InterruptedException** is handled separately with thread interruption
- **Query execution times** are tracked and logged for performance monitoring
- **Error streams** are properly captured and returned for better diagnostics
- **Platform-aware error handling** gracefully handles macOS-specific tables on other systems
- **Correct table names** verified against actual osquery schema (e.g., `fan_speed_sensors` not `fan_control_sensors`)

## MCP Client Implementation

The project includes a complete MCP client implementation using Spring AI's autoconfiguration:

### Spring AI MCP Client (`client-springai/` directory)

A simplified implementation using Spring AI's MCP autoconfiguration that demonstrates:

#### Key Features
- **Minimal Code**: Clean ~150 line implementation with maximum functionality
- **Zero Protocol Code**: No JSON-RPC handling, automatic MCP negotiation
- **Declarative Configuration**: YAML-based setup for easy maintenance
- **Auto Tool Discovery**: Tools discovered via `SyncMcpToolCallbackProvider`
- **Framework Integration**: Built-in error handling, timeouts, and process management

#### Critical Configuration
```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        toolcallback:
          enabled: true  # ESSENTIAL for tool discovery
        type: SYNC
        stdio:
          connections:
            osquery-server:
              command: java
              args: [-jar, ../build/libs/OsqueryMcpServer-1.0.jar]
```

#### Tool Naming Convention
Spring AI MCP automatically prefixes tool names:
- **Format**: `{client_name}_{server_name}_{tool_name}`
- **Example**: `osquery_cli_osquery_server_getSystemHealthSummary`
- **Auto-discovery**: All 9 tools discovered automatically

#### Usage Examples
```bash
# Build and test
./gradlew bootJar && cd client-springai && ./gradlew build

# Quick test (formatted output by default)
./gradlew run --args="\"system health\""

# Raw JSON output
./gradlew run --args="--raw \"system health\""

# Interactive mode
./gradlew run --args="--interactive"
# Toggle raw/formatted with 'raw' command in interactive mode

# Manual testing
./test-client-springai.sh
```

#### Output Formatting (NEW)
The Spring AI client now includes intelligent output formatting:
- **Table format**: Process lists, network connections as ASCII tables
- **Key-value format**: System info as aligned key-value pairs  
- **Pretty JSON**: Complex nested data with proper indentation
- **Raw mode**: Original JSON with `--raw` flag or 'raw' command

#### Implementation Notes
- **SyncMcpToolCallbackProvider**: Injected dependency providing all discovered tools
- **Automatic Prefixing**: Tool names include client/server identifiers
- **Modern Jackson APIs**: Uses current `properties().iterator()` instead of deprecated methods
- **Clean Code**: Removed unused methods and parameters for maintainability
- **Performance**: Matches Claude Desktop performance (1-3 seconds per query)
- **Reliability**: Production-tested Spring AI framework handles all protocol details

## Common Tasks

### Build and Run
```bash
./gradlew build          # Build the server
./gradlew bootRun        # Run server locally
./gradlew test           # Run server tests

# Client operations
cd client-springai
../gradlew build         # Build the Spring AI client
../gradlew test          # Run client tests
../gradlew run --args="\"query\"" # Run client with query
```

### MCP Integration
The server integrates with Claude Desktop via configuration like:
```json
{
  "mcpServers": {
    "osquery": {
      "command": "java",
      "args": ["-jar", "path/to/osquery-mcp-server.jar"]
    }
  }
}
```

### Prerequisites
- Java 21+
- Osquery installed (`osqueryi` in PATH)
- The service validates osquery availability at startup

## Design Philosophy

1. **Simplicity**: Keep the core implementation focused and lightweight
2. **Natural Language Focus**: Tools should enable AI to answer human questions
3. **Diagnostics-First**: Prioritize common troubleshooting scenarios
4. **Schema Discovery**: Help AI understand available data structures
5. **Example-Driven**: Provide query templates for common use cases

## Avoid Over-Engineering

Previous versions attempted to add extensive security, caching, and production features. The current design intentionally keeps things simple because:
- This is for local AI assistant use, not production services
- Osquery itself provides the security model
- The LLM translates natural language to appropriate queries
- Complex security measures would interfere with the diagnostic purpose

## Commit Guidelines

When committing changes:
- Use descriptive commit messages explaining the "why"
- Include the Claude Code footer:
  ```
  🤖 Generated with [Claude Code](https://claude.ai/code)
  
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```
- Test thoroughly before committing
- Update this CLAUDE.md file if project structure changes significantly