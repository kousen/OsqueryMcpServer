# Skill vs MCP: Choosing the Right Approach

This document compares the two approaches for integrating osquery with AI assistants: Claude Code Skills and MCP Servers.

## Quick Summary

| Approach | Best For |
|----------|----------|
| **Claude Code Skill** | Quick personal utilities, simple CLI wrappers, Claude Code users |
| **MCP Server** | Shared tooling, multiple AI clients, formal schemas, server-side logic |

## Detailed Comparison

| Aspect | Claude Code Skill | MCP Server |
|--------|------------------|------------|
| **Availability** | Claude Code only | Any MCP client (Claude Desktop, IDE plugins, custom apps) |
| **Startup** | Instant (no process) | JVM startup time |
| **Complexity** | Markdown files | Spring Boot application |
| **Tool discovery** | Semantic matching on description | Formal tool registry with schemas |
| **Type safety** | Claude interprets parameters | Defined parameter types & validation |
| **State between calls** | Stateless | Can maintain state in server |
| **Error handling** | Claude interprets command output | Structured error responses |
| **Permissions** | Can restrict tools (`allowed-tools: Bash`) | All tools available to client |
| **Extensibility** | Claude Code only | Any MCP client |
| **Testing** | Manual testing | Unit/integration tests with JUnit |
| **Versioning** | Markdown files in git | Semantic versioning, JAR distribution |
| **Maintenance** | Edit markdown, restart Claude Code | Rebuild JAR, restart server |
| **Distribution** | Copy folder or commit to repo | JAR file + MCP configuration |

## When to Choose a Skill

Choose a Claude Code Skill when:

- You're building **personal utilities** for your own workflow
- The tool is a **simple CLI wrapper** (like osquery)
- You want **conversational guidance** baked into the tool
- You need **zero startup overhead**
- The functionality is **stateless** between calls
- You're only using **Claude Code** (not Claude Desktop)

### Skill Advantages

1. **Simplicity**: Just markdown files - no build process, no dependencies
2. **Speed**: No server process to start, queries run immediately
3. **Context**: Can include baseline guidance ("Is 50% CPU normal?")
4. **Maintenance**: Edit text files and restart - no recompilation
5. **Portability**: Copy a folder to share or back up

## When to Choose MCP

Choose an MCP Server when:

- You need the tool in **Claude Desktop** or other MCP clients
- Multiple team members will **share the tooling**
- You need **formal parameter validation** and schemas
- The server needs to **maintain state** between calls
- You want **structured error handling** with proper error codes
- You need **server-side logic** (caching, rate limiting, authentication)
- You want **comprehensive testing** with unit tests

### MCP Advantages

1. **Universality**: Works with any MCP-compatible client
2. **Type Safety**: Formal tool schemas with parameter types
3. **Statefulness**: Server can cache results, track sessions
4. **Testing**: Full test suite with JUnit, mocking, integration tests
5. **Enterprise Ready**: Proper versioning, distribution, monitoring

## For Osquery Specifically

For the osquery use case, the **Skill is arguably the better fit** because:

1. **Read-only queries**: Osquery is inherently read-only - no state to manage
2. **Direct CLI works**: `osqueryi --json "SQL"` is simple and reliable
3. **Knowledge-centric**: The value is in knowing *which* queries to run, not the transport
4. **Conversational context**: Explaining what's "normal" is as valuable as the data
5. **Zero overhead**: No JVM startup for quick diagnostic checks

The **MCP server is still valuable** for:

1. Claude Desktop users who want osquery integration
2. Building automated monitoring systems
3. Integration with other MCP-compatible tools
4. When you need query validation or caching

## Hybrid Approach

This project provides both approaches, allowing you to:

- Use the **Skill in Claude Code** for quick, conversational diagnostics
- Use the **MCP Server in Claude Desktop** for the same functionality
- Share the **query knowledge** between both implementations

The query templates in the Skill mirror the MCP server's tools, ensuring consistent behavior regardless of which approach you use.

## File Locations

```
# Claude Code Skill
.claude/skills/osquery/
├── SKILL.md         # Skill definition and triggers
└── queries.md       # Query templates and guidance

# MCP Server
src/main/java/com/kousenit/osquerymcpserver/
├── OsqueryMcpServerApplication.java
└── OsqueryService.java    # @Tool annotated methods
```

## See Also

- [Claude Code Skills Documentation](https://docs.anthropic.com/en/docs/claude-code/skills)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/api/mcp.html)
