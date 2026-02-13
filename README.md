# Osquery MCP Server, Client & Skill

A complete implementation for integrating [Osquery](https://osquery.io/) with AI assistants, providing three approaches: an MCP server for Claude Desktop, a Spring AI client, and a Claude Code skill for direct CLI usage.

## Overview

This project enables AI assistants to answer system diagnostic questions like "Why is my fan running so hot?" or "What's using all my memory?" by translating natural language into Osquery SQL queries.

**Three ways to use osquery with AI:**

| Approach | Best For | How It Works |
|----------|----------|--------------|
| **MCP Server** | Claude Desktop | Spring Boot server communicates via MCP protocol |
| **Spring AI Client** | Programmatic access | CLI client using Spring AI's MCP auto-configuration |
| **Claude Code Skill** | Claude Code CLI | Direct `osqueryi` execution via Bash, no server needed |

## What's New (upgrade/spring-ai-2.0 branch)

This branch upgrades the entire stack to the latest Spring ecosystem and adds GraalVM native image support:

| Component | Before (main) | After (this branch) |
|-----------|---------------|---------------------|
| Spring Boot | 3.5.0 | **4.0.1** |
| Spring AI | 1.0.0 | **2.0.0-M2** |
| Java | 21 | **25** (GraalVM CE) |
| Jackson | 2.x (`com.fasterxml`) | **3.x** (`tools.jackson`) |
| Dependency mgmt | `io.spring.dependency-management` plugin | Gradle `platform()` BOMs |
| Native image | Not supported | **GraalVM native binary (~36ms startup)** |
| System health | Sequential (5 queries) | **Parallel via virtual threads** |

### Key Upgrade Details

**GraalVM Native Image**: The MCP server compiles to a ~62MB native binary that starts and responds to MCP requests in ~36ms. This is critical for the voice interface use case — when a JavaFX voice client launches the server, it needs to respond instantly.

**Virtual Threads**: `getSystemHealthSummary()` now runs all 5 diagnostic queries (CPU, memory, disk, network, temperature) in parallel using `Executors.newVirtualThreadPerTaskExecutor()` with `CompletableFuture.supplyAsync()`. This reduces response time from the sum of all queries to the duration of the slowest single query.

**Jackson 3 Migration** (client only): Spring Boot 4 ships Jackson 3 with new Maven coordinates (`tools.jackson.core` instead of `com.fasterxml.jackson.core`), immutable builders (`JsonMapper.builder().build()` instead of `new ObjectMapper()`), and unchecked exceptions (`JacksonException` instead of `JsonProcessingException`).

**Gradle Build Changes**: Spring Boot 4 drops the `io.spring.dependency-management` plugin. Dependencies are now managed with Gradle-native `platform()` BOMs and the Spring Milestones repository (`https://repo.spring.io/milestone`).

## Features

### MCP Server
- **Natural Language System Diagnostics**: Ask questions like "What's using my CPU?" and get intelligent answers
- **11 Specialized Tools** for common diagnostic scenarios:
  - Execute custom Osquery SQL queries
  - Get table schemas and available columns
  - Find high CPU/memory/disk I/O usage processes
  - Analyze network connections
  - Check system temperature and fan speeds (macOS)
  - Identify suspicious processes
  - Get comprehensive system health summary (parallel execution)
  - Access example queries for common problems
- **Smart Query Assistance**: Built-in examples and schema discovery help the AI construct better queries
- **STDIO-based MCP Integration**: Works seamlessly with Claude Desktop and other MCP-compatible AI tools
- **Spring Boot 4.0.1 with Java 25**: Latest Spring ecosystem with GraalVM native image support
- **GraalVM Native Image**: Sub-200ms startup for instant MCP responses (~36ms measured)

### Spring AI MCP Client
- **Spring AI Auto-Configuration**: Leverages Spring AI 2.0's MCP client starter for zero-configuration setup
- **Interactive CLI**: REPL interface for exploratory system diagnostics
- **Natural Language Processing**: Maps human questions to appropriate server tools
- **Custom SQL Support**: Execute direct osquery commands through the MCP server
- **Automatic Tool Discovery**: Tools discovered via `SyncMcpToolCallbackProvider` injection
- **Built-in Error Handling**: Framework-managed timeouts and process management
- **Declarative Configuration**: YAML-based setup for easy maintenance
- **Jackson 3**: Uses immutable `JsonMapper` builder pattern and modern APIs
- **Comprehensive Testing**: Includes automated unit tests for query mapping logic

### Claude Code Skill
- **Zero Overhead**: No server process required - runs `osqueryi` directly via Bash
- **Natural Language Triggers**: Automatically activates for system diagnostic questions
- **Predefined Query Templates**: Same diagnostic queries as the MCP server
- **Baseline Guidance**: Includes "is this normal?" context for interpreting results
- **Security Explanations**: Explains what makes processes suspicious (and common false positives)
- **Platform Awareness**: Notes macOS vs Linux differences
- **Easy Maintenance**: Just markdown files - edit and restart Claude Code

## Performance & Reliability

- **Native Image Startup**: ~36ms to first MCP response (vs several seconds for JVM startup)
- **Parallel Queries**: System health summary runs 5 queries concurrently via virtual threads
- **Query Timeouts**: Prevents hanging with 30-second timeout for queries, 5-second for version checks
- **Process Management**: Uses ProcessBuilder for robust resource handling and proper cleanup
- **Execution Time Logging**: Tracks query performance for monitoring and debugging
- **Error Handling**: Captures and returns detailed error messages from failed queries
- **Resource Safety**: Automatically destroys processes that exceed timeout limits

## Prerequisites

- **Java 25+** (GraalVM CE 25 recommended for native image support)
  - Install via SDKMAN: `sdk install java 25.0.2-graalce`
- [Osquery](https://osquery.io/downloads/official) installed and `osqueryi` available in your PATH
- Gradle (or use the included Gradle wrapper)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/OsqueryMcpServer.git
cd OsqueryMcpServer
git checkout upgrade/spring-ai-2.0   # For the latest version
```

2. Build the project:
```bash
./gradlew build        # Build server + client, run all tests
./gradlew bootJar      # Create executable JAR
cd client-springai && ../gradlew build  # Build Spring AI client
```

3. Build the native image (optional, recommended):
```bash
sdk use java 25.0.2-graalce
./gradlew nativeCompile --no-configuration-cache
# Binary at: build/native/nativeCompile/OsqueryMcpServer
```

4. Run the server:
```bash
# JVM mode
./gradlew bootRun

# Native mode (instant startup)
./build/native/nativeCompile/OsqueryMcpServer
```

5. Test the Spring AI MCP client:
```bash
# Natural language queries
cd client-springai && ../gradlew run --args="\"What's using my CPU?\""

# Interactive mode
../gradlew run --args="--interactive"

# Custom SQL queries
../gradlew run --args="\"SELECT name FROM system_info\""

# Run test suite
./test-client-springai.sh
```

6. Run tests:
```bash
./gradlew :test                              # Server tests
./gradlew :client-springai:test              # Spring AI client tests
./gradlew build                              # All tests
```

## Usage

### MCP Server
The server operates in STDIO mode and provides eleven specialized tools for system diagnostics:

### Spring AI MCP Client
The client provides multiple ways to interact with the server:

#### Natural Language Queries
```bash
cd client-springai
../gradlew run --args="\"What's using my CPU?\""
../gradlew run --args="\"Show network connections\""
../gradlew run --args="\"Why is my fan running?\""
../gradlew run --args="\"Show system health\""
../gradlew run --args="\"Check for suspicious processes\""
../gradlew run --args="\"Show high disk I/O processes\""
```

#### Custom SQL Queries
```bash
../gradlew run --args="\"SELECT name, pid, cpu_time FROM processes ORDER BY cpu_time DESC LIMIT 5\""
../gradlew run --args="\"SELECT * FROM system_info\""
```

#### Interactive Mode
```bash
../gradlew run --args="--interactive"
# Then type queries interactively, 'help' for assistance, 'exit' to quit
```

### Claude Code Skill

The skill activates automatically when you ask system diagnostic questions in Claude Code:

```
> Why is my computer slow?
> What's using all my memory?
> Show me network connections
> Are there any suspicious processes?
> Why is my fan running?
```

#### Installation

**Option 1: Project-level (included in this repo)**
```bash
# Already available in .claude/skills/osquery/ when working in this project
```

**Option 2: Personal (works across all projects)**
```bash
cp -r .claude/skills/osquery ~/.claude/skills/
# Restart Claude Code to load the skill
```

#### How It Works

The skill guides Claude to run `osqueryi` commands directly:
```bash
osqueryi --json "SELECT name, pid, resident_size FROM processes ORDER BY resident_size DESC LIMIT 10"
```

No server required - Claude executes queries via Bash and interprets the JSON results.

### Server Tools Available

#### Core Tools
- **`executeOsquery(sql)`**: Execute any valid Osquery SQL query
- **`listOsqueryTables()`**: Get all available Osquery tables on your system
- **`getTableSchema(tableName)`**: Discover columns and types for any table

### Diagnostic Tools
- **`getHighCpuProcesses()`**: Find processes consuming the most CPU
- **`getHighMemoryProcesses()`**: Find processes using the most memory
- **`getHighDiskIOProcesses()`**: Find processes with high disk read/write activity
- **`getNetworkConnections()`**: Show active network connections with process info
- **`getTemperatureInfo()`**: Get system temperature and fan speeds (macOS)
- **`getSuspiciousProcesses()`**: Identify processes with unusual characteristics

### Helper Tools
- **`getCommonQueries()`**: Get example queries for common diagnostic scenarios
- **`getSystemHealthSummary()`**: Get comprehensive overview of CPU, memory, disk, network, and temperature (runs all queries in parallel via virtual threads)

## Example AI Interactions

Instead of writing complex SQL, you can now ask natural language questions:

**"Why is my computer running slowly?"** -> AI uses `getHighCpuProcesses()` and `getHighMemoryProcesses()`

**"What's connecting to the internet?"** -> AI uses `getNetworkConnections()`

**"Why is my fan so loud?"** -> AI uses `getTemperatureInfo()` to check system temps

**"Show me all Chrome processes"** -> AI uses `executeOsquery()` with schema discovery

**"Give me an overall system health check"** -> AI uses `getSystemHealthSummary()` for comprehensive diagnostics (5 queries run in parallel)

**"Is my system compromised?"** -> AI uses `getSuspiciousProcesses()` to check for anomalies

## Configuration

The application is configured through `src/main/resources/application.properties`:

- **Server Name**: osquery-server
- **Version**: 1.0.0
- **Mode**: SYNC (synchronous operation)
- **Transport**: STDIO (standard input/output)

## MCP Integration

This server implements the Model Context Protocol (MCP) using Spring AI's MCP Server starter. It can be integrated with AI tools that support MCP, such as:

- Claude Desktop App
- Other MCP-compatible AI assistants

### Example MCP Configuration

For Claude Desktop, add to your configuration:

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

Or with the native binary for instant startup (~36ms):

```json
{
  "mcpServers": {
    "osquery": {
      "command": "path/to/OsqueryMcpServer"
    }
  }
}
```

## Security Considerations

> **Warning**: This server executes system commands with the privileges of the running user. Consider the following security measures:

- Run with minimal required privileges
- Implement query filtering or whitelisting in production
- Monitor and log all executed queries
- Consider using read-only Osquery queries

## Development

### Project Architecture

```
src/                                    # MCP Server (Spring Boot 4)
├── main/java/com/kousenit/osquerymcpserver/
│   ├── OsqueryMcpServerApplication.java      # Main application
│   └── OsqueryService.java                   # MCP tools (virtual threads)
└── test/java/com/kousenit/osquerymcpserver/
    └── OsqueryServiceTest.java               # Server tests

client-springai/                        # Spring AI 2.0 MCP Client
├── src/main/java/com/kousenit/osqueryclient/springai/
│   └── SpringAiOsqueryClientApplication.java # CLI application (Jackson 3)
├── src/test/java/com/kousenit/osqueryclient/springai/
│   └── QueryMappingTest.java                 # Unit tests
├── application.yml                          # Spring AI configuration
└── test-client-springai.sh                  # Test runner

.claude/skills/osquery/                 # Claude Code Skill
├── SKILL.md                                 # Skill definition & triggers
└── queries.md                               # Query templates & baselines

build.gradle.kts                            # Server build (GraalVM native)
```

### Build Configuration

The project uses Gradle with `platform()` BOMs for dependency management (Spring Boot 4 drops the `io.spring.dependency-management` plugin):

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("org.graalvm.buildtools.native") version "0.10.6"  // Server only
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.1"))
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M2"))
    // ...
}
```

### Running Tests

```bash
./gradlew :test                          # Server tests
./gradlew :client-springai:test          # Spring AI client tests
./gradlew build                          # All tests
./test-client-springai.sh                # Full client test suite
```

### Building the Native Image

```bash
# Requires GraalVM CE 25
sdk install java 25.0.2-graalce
sdk use java 25.0.2-graalce

# Build (takes ~25 seconds)
./gradlew nativeCompile --no-configuration-cache

# Test
./build/native/nativeCompile/OsqueryMcpServer
```

**Note**: The `--no-configuration-cache` flag is required due to a known incompatibility between the GraalVM buildtools plugin 0.10.6 and Gradle 9's configuration cache serialization.

## Built-in Diagnostic Queries

The server includes pre-built queries for common diagnostic scenarios. Use `getCommonQueries()` to see all available examples:

### Performance Analysis
```sql
-- Top CPU consuming processes
SELECT name, pid, uid, (user_time + system_time) AS cpu_time FROM processes ORDER BY cpu_time DESC LIMIT 10;

-- Memory usage by process
SELECT name, pid, resident_size, total_size FROM processes ORDER BY resident_size DESC LIMIT 10;
```

### Network Analysis
```sql
-- Active network connections
SELECT pid, local_address, local_port, remote_address, remote_port, state
FROM process_open_sockets WHERE state = 'ESTABLISHED'
```

### System Information
```sql
-- Overall system info
SELECT hostname, cpu_brand, physical_memory, hardware_vendor, hardware_model FROM system_info;

-- Recent file changes
SELECT path, mtime, size FROM file WHERE path LIKE '/Users/%'
AND mtime > (strftime('%s', 'now') - 3600)
```

The AI can use these as templates or call the specialized diagnostic tools directly.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License. See [License](LICENSE) for details.

## Acknowledgments

- [Osquery](https://osquery.io/) by Facebook
- [Spring AI MCP](https://spring.io/projects/spring-ai) for MCP protocol implementation
- Spring Boot framework
- GraalVM for native image compilation
