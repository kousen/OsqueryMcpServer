# Osquery MCP Server

A Spring Boot application that provides a Model Context Protocol (MCP) server interface for [Osquery](https://osquery.io/), enabling AI tools to query system information using SQL.

## Overview

The Osquery MCP Server bridges AI models and Osquery's powerful system introspection capabilities. It exposes Osquery functionality through the MCP protocol, allowing AI assistants like Claude to execute system queries and retrieve detailed information about the operating system, running processes, network connections, and more.

## Features

- Execute arbitrary Osquery SQL queries through MCP tools
- List available Osquery tables
- Synchronous STDIO-based communication
- Spring Boot 3.5 with Java 21 support
- GraalVM Native Image compatible

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

The server operates in STDIO mode and provides two main tools:

### 1. Execute Osquery SQL
Execute any valid Osquery SQL query:
```sql
SELECT name, pid, uid FROM processes WHERE name LIKE '%java%'
```

### 2. List Osquery Tables
Get a list of all available Osquery tables on your system:
```
.tables
```

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

### Building Native Image

The project includes GraalVM Native Image support:

```bash
./gradlew nativeCompile
```

## Example Queries

Here are some useful Osquery queries you can execute through this server:

```sql
-- List all running processes
SELECT name, pid, uid, cmdline FROM processes;

-- Show network connections
SELECT * FROM listening_ports;

-- Get system information
SELECT * FROM system_info;

-- List installed packages (macOS)
SELECT name, version FROM homebrew_packages;

-- Check user accounts
SELECT username, uid, gid, directory FROM users;
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License. See [License](LICENSE) for details.

## Acknowledgments

- [Osquery](https://osquery.io/) by Facebook
- [Spring AI MCP](https://spring.io/projects/spring-ai) for MCP protocol implementation
- Spring Boot framework