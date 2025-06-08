# Osquery MCP Server

A Spring Boot application that provides a Model Context Protocol (MCP) server interface for [Osquery](https://osquery.io/), enabling AI assistants to answer system diagnostic questions using natural language.

## Overview

The Osquery MCP Server acts as an intelligent bridge between AI models and your operating system. It translates natural language questions like "Why is my fan running so hot?" or "What's using all my memory?" into precise Osquery SQL queries, allowing AI assistants to diagnose system issues, monitor performance, and investigate security concerns.

## Features

- **Natural Language System Diagnostics**: Ask questions like "What's using my CPU?" and get intelligent answers
- **8 Specialized Tools** for common diagnostic scenarios:
  - Execute custom Osquery SQL queries
  - Get table schemas and available columns
  - Find high CPU/memory usage processes
  - Analyze network connections
  - Check system temperature and fan speeds (macOS)
  - Access example queries for common problems
- **Smart Query Assistance**: Built-in examples and schema discovery help the AI construct better queries
- **STDIO-based MCP Integration**: Works seamlessly with Claude Desktop and other MCP-compatible AI tools
- **Spring Boot 3.5 with Java 21**: Modern, efficient, and maintainable codebase using Java 17+ features
- **Comprehensive Testing**: Includes automated tests with debug logging support

## Performance & Reliability

- **Query Timeouts**: Prevents hanging with 30-second timeout for queries, 5-second for version checks
- **Process Management**: Uses ProcessBuilder for robust resource handling and proper cleanup
- **Execution Time Logging**: Tracks query performance for monitoring and debugging
- **Error Handling**: Captures and returns detailed error messages from failed queries
- **Resource Safety**: Automatically destroys processes that exceed timeout limits

## Prerequisites

- Java 21 or higher
- [Osquery](https://osquery.io/downloads/official) installed and `osqueryi` available in your PATH
- Gradle (or use the included Gradle wrapper)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/OsqueryMcpServer.git
cd OsqueryMcpServer
```

2. Build the project:
```bash
./gradlew build
```

3. Run the application:
```bash
./gradlew bootRun
```

## Usage

The server operates in STDIO mode and provides eight specialized tools for system diagnostics:

### Core Tools
- **`executeOsquery(sql)`**: Execute any valid Osquery SQL query
- **`listOsqueryTables()`**: Get all available Osquery tables on your system
- **`getTableSchema(tableName)`**: Discover columns and types for any table

### Diagnostic Tools
- **`getHighCpuProcesses()`**: Find processes consuming the most CPU
- **`getHighMemoryProcesses()`**: Find processes using the most memory
- **`getNetworkConnections()`**: Show active network connections with process info
- **`getTemperatureInfo()`**: Get system temperature and fan speeds (macOS)

### Helper Tools
- **`getCommonQueries()`**: Get example queries for common diagnostic scenarios

## Example AI Interactions

Instead of writing complex SQL, you can now ask natural language questions:

**"Why is my computer running slowly?"** → AI uses `getHighCpuProcesses()` and `getHighMemoryProcesses()`

**"What's connecting to the internet?"** → AI uses `getNetworkConnections()`

**"Why is my fan so loud?"** → AI uses `getTemperatureInfo()` to check system temps

**"Show me all Chrome processes"** → AI uses `executeOsquery()` with schema discovery

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

## Security Considerations

⚠️ **Warning**: This server executes system commands with the privileges of the running user. Consider the following security measures:

- Run with minimal required privileges
- Implement query filtering or whitelisting in production
- Monitor and log all executed queries
- Consider using read-only Osquery queries

## Development

### Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/kousenit/osquerymcpserver/
│   │   │       ├── OsqueryMcpServerApplication.java
│   │   │       └── OsqueryService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
└── build.gradle.kts
```

### Running Tests

```bash
./gradlew test
```


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