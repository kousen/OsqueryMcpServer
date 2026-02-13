# Claude AI Assistant Instructions

This document provides context and instructions for AI assistants working on the Osquery MCP Server project.

## Project Purpose

The **Osquery MCP Server** is a Spring Boot application that acts as an intelligent bridge between AI models and the operating system. It translates natural language questions like "Why is my fan running so hot?" or "What's using all my memory?" into precise Osquery SQL queries, enabling AI assistants to diagnose system issues, monitor performance, and investigate security concerns.

The project now includes a **complete Spring AI MCP client implementation** that demonstrates how to communicate with the server through the Model Context Protocol using Spring AI's auto-configuration.

**Key Point**: This is NOT a production service exposed to untrusted users. It's designed for local use by AI assistants to help with system diagnostics through natural language interaction.

**Bigger Picture**: A JavaFX voice client (in `~/Documents/AI/starfleet-voice-interface`) transcribes audio and connects to this MCP server. The end goal is Star Trek-style interaction: hold a button, say "Computer, run a level 1 diagnostic," and the MCP server does the work. This makes native image startup time critical — the server needs to respond instantly when the voice client launches it. The all-Java stack (JavaFX client + Spring Boot MCP server + GraalVM native binary) is a key architectural advantage.

## Architecture

- **Spring Boot 4.0.1** with **Java 25** (GraalVM CE 25)
- **Spring AI 2.0.0-M2** for MCP protocol support
- **Model Context Protocol (MCP)** server using Spring AI's MCP starter
- **STDIO-based communication** for integration with Claude Desktop and other MCP tools
- **GraalVM native image support** — ~36ms startup for instant MCP responses
- **11 specialized diagnostic tools** exposed via `@Tool` annotations
- **Virtual threads** for parallel query execution in `getSystemHealthSummary()`
- **ProcessBuilder** for robust process management with proper resource handling
- **Query timeouts**: 30 seconds for queries, 5 seconds for version checks
- **Execution time logging** for performance monitoring
- **Jackson 3** (`tools.jackson` packages) in the client

## Branch Strategy

- **`main`**: Stable release on Spring Boot 3.5 / Spring AI 1.0 / Java 21
- **`upgrade/spring-ai-2.0`**: Spring Boot 4.0.1 / Spring AI 2.0.0-M2 / Java 25 / GraalVM native image — will merge when Spring AI 2.0 goes GA

## Available Tools

### Core Tools
- `executeOsquery(sql)` - Execute any Osquery SQL query
- `listOsqueryTables()` - List all available Osquery tables
- `getTableSchema(tableName)` - Get column information for any table

### Diagnostic Tools
- `getHighCpuProcesses()` - Find CPU-intensive processes
- `getHighMemoryProcesses()` - Find memory-intensive processes
- `getHighDiskIOProcesses()` - Find processes with high disk I/O activity
- `getNetworkConnections()` - Show active network connections
- `getTemperatureInfo()` - System temperature and fan speeds (macOS)
- `getSuspiciousProcesses()` - Identify potentially suspicious processes

### Helper Tools
- `getCommonQueries()` - Example queries for common scenarios
- `getSystemHealthSummary()` - Comprehensive overview of CPU, memory, disk, network, and temperature (runs 5 queries in parallel via virtual threads)

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
- Run server tests with: `./gradlew :test`
- Run client tests with: `./gradlew :client-springai:test`
- Run all tests with: `./gradlew build`

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
- **Virtual threads** in `getSystemHealthSummary()` run 5 osquery calls in parallel via `Executors.newVirtualThreadPerTaskExecutor()` and `CompletableFuture.supplyAsync()`

## Build Configuration

### Dependency Management (Spring Boot 4 / upgrade branch)

Spring Boot 4 drops the `io.spring.dependency-management` plugin. Dependencies are managed with Gradle-native `platform()` BOMs:

```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.1"))
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M2"))
    // ...
}
```

Spring AI 2.0.0-M2 requires the Spring Milestones repository:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}
```

### GraalVM Native Image

The server supports GraalVM native image compilation for sub-200ms startup:

```bash
# Requires GraalVM CE 25 (install via SDKMAN: sdk install java 25.0.2-graalce)
sdk use java 25.0.2-graalce
./gradlew nativeCompile --no-configuration-cache
```

The `--no-configuration-cache` flag is required due to a known incompatibility between the GraalVM buildtools plugin 0.10.6 and Gradle 9's configuration cache.

**Performance**: The native binary starts and responds to MCP requests in ~36ms (vs several seconds for JVM startup).

**Binary location**: `build/native/nativeCompile/OsqueryMcpServer`

### Jackson 3 (Client)

Spring Boot 4 ships Jackson 3 with new Maven coordinates and API changes:
- **Group ID**: `com.fasterxml.jackson.core` → `tools.jackson.core`
- **ObjectMapper**: `new ObjectMapper()` → `JsonMapper.builder().build()` (immutable builder)
- **Exceptions**: `JsonProcessingException` (checked) → `JacksonException` (unchecked)
- **Import prefix**: `com.fasterxml.jackson` → `tools.jackson`
- **API**: `fieldNames()` → `propertyNames()` (returns `Iterable` instead of `Iterator`)
- **Stable API**: `properties().iterator()` works unchanged in Jackson 3

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
- **Auto-discovery**: All 11 tools discovered automatically

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

#### Output Formatting
The Spring AI client includes intelligent output formatting:
- **Table format**: Process lists, network connections as ASCII tables
- **Key-value format**: System info as aligned key-value pairs
- **Pretty JSON**: Complex nested data with proper indentation
- **Raw mode**: Original JSON with `--raw` flag or 'raw' command

#### Implementation Notes
- **SyncMcpToolCallbackProvider**: Injected dependency providing all discovered tools
- **Automatic Prefixing**: Tool names include client/server identifiers
- **Jackson 3 APIs**: Uses `JsonMapper.builder().build()`, `propertyNames()`, and `JacksonException`
- **Clean Code**: Removed unused methods and parameters for maintainability
- **Performance**: Matches Claude Desktop performance (1-3 seconds per query)
- **Reliability**: Production-tested Spring AI framework handles all protocol details

## Common Tasks

### Build and Run
```bash
./gradlew build          # Build the server
./gradlew bootRun        # Run server locally
./gradlew test           # Run server tests

# Native image
sdk use java 25.0.2-graalce
./gradlew nativeCompile --no-configuration-cache
./build/native/nativeCompile/OsqueryMcpServer  # Run native binary

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

Or with the native binary for instant startup:
```json
{
  "mcpServers": {
    "osquery": {
      "command": "path/to/OsqueryMcpServer"
    }
  }
}
```

### Prerequisites
- Java 25+ (GraalVM CE 25 recommended for native image support)
- Osquery installed (`osqueryi` in PATH)
- The service validates osquery availability at startup

## Design Philosophy

1. **Simplicity**: Keep the core implementation focused and lightweight
2. **Natural Language Focus**: Tools should enable AI to answer human questions
3. **Diagnostics-First**: Prioritize common troubleshooting scenarios
4. **Schema Discovery**: Help AI understand available data structures
5. **Example-Driven**: Provide query templates for common use cases
6. **Instant Startup**: GraalVM native image for sub-200ms response times

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
