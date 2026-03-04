# Presentation Plan: Voice-Controlled System Diagnostics with Spring AI and MCP

**Duration**: 30 minutes (code walkthrough format)
**Audience**: Java developers with Spring experience
**Projects**: OsqueryMcpServer + starfleet-voice-interface (JavaFX voice client)
**Video reference**: "Takes from the Jar Side" YouTube channel

---

## Narrative Arc

**The Big Idea**: You can build a complete voice-controlled system diagnostic tool using Java, Spring Boot, and the Model Context Protocol — the same stack you already know. The LLM is the "brains," osquery provides the "eyes into your OS," and MCP is the standard protocol that connects them.

---

## Outline (30 minutes)

### 1. Opening Hook (2 min)

**Live demo first** — don't explain, just show:
- Launch the JavaFX voice interface
- Say "Why is my fan running so hot?" into the microphone
- Watch it: transcribe speech → call LLM → execute osquery via MCP → speak the answer back
- "Everything you just saw is Java. Let me show you how it works."

This immediately establishes the "wow factor" and gives the audience a mental model for the rest of the talk.

### 2. The Architecture — What Just Happened? (3 min)

Walk through the flow that just executed, layer by layer:

```
Voice Input (JavaFX + STT)
    → LLM (interprets intent)
        → MCP Protocol (standard tool-calling interface)
            → Spring Boot MCP Server (exposes @Tool methods)
                → Osquery (reads OS state, returns JSON)
            ← Structured response
        ← Natural language answer
    ← TTS Output (spoken response)
```

Key points for this audience:
- **MCP** is an open protocol (like JDBC for AI tools) — mention it's from Anthropic but vendor-neutral
- The MCP server is just a Spring Boot app with `@Tool` annotations — they already know this pattern
- Osquery is "SQL for your operating system" — read-only, safe, cross-platform

### 3. The MCP Server — Where Spring Developers Feel at Home (8 min)

**Code walkthrough of `OsqueryService.java`**:

- **`@Tool` annotations** — show how Spring AI exposes methods as MCP tools. Compare to `@GetMapping` — same idea, different protocol.

  ```java
  @Tool(description = "Execute an Osquery SQL query and return results as JSON")
  public String executeOsquery(String sql) { ... }
  ```

- **The diagnostic tools** — walk through 2-3 examples:
  - `getHighCpuProcesses()` — show the SQL text block, the ProcessBuilder call, timeout handling
  - `getSystemHealthSummary()` — aggregates multiple queries, show how the LLM uses this as a "first pass"
  - `getTemperatureInfo()` — platform-aware error handling (macOS tables on Linux)

- **`OsqueryMcpServerApplication.java`** — show how tiny it is. The `ToolCallbackProvider` bean is the bridge. "That's it. That's the entire MCP server setup."

- **Configuration** — `application.properties`: `spring.ai.mcp.server.stdio=true`. Explain STDIO transport briefly (stdin/stdout JSON-RPC, designed for local tools).

**Key message**: If you can write a Spring `@Service`, you can write an MCP server. The framework handles the protocol.

### 4. The Spring AI Client — Calling MCP from Java (5 min)

**Code walkthrough of `SpringAiOsqueryClientApplication.java`**:

- **Auto-configuration magic** — `SyncMcpToolCallbackProvider` is injected, zero protocol code
- **`application.yml`** — show the declarative MCP client config:
  - `toolcallback.enabled: true` — "This one line is critical; without it, no tools are discovered"
  - STDIO connection pointing at the server JAR
- **Natural language → tool mapping** — walk through the `determineToolName()` method. Keyword matching maps "what's eating my CPU?" to `getHighCpuProcesses`. Note: in practice, the LLM handles this mapping; the client just demonstrates the concept.
- **Output formatting** — briefly show the ASCII table renderer. "The LLM returns JSON; we make it human-readable."

**Key message**: Spring AI's MCP client auto-configuration means you write ~0 lines of protocol code. Configuration over code.

### 5. The Voice Interface — JavaFX Meets AI (5 min)

**Code walkthrough of key parts of starfleet-voice-interface**:

- **Speech-to-Text** — how voice input gets transcribed (which API/service)
- **LLM integration** — how the transcribed text gets sent to the model, which decides which MCP tools to call
- **Text-to-Speech** — how the response gets spoken back
- **JavaFX UI** — the visual interface, any Star Trek theming

**Key message**: JavaFX is alive and well for desktop AI applications. Voice adds a natural interaction layer that makes diagnostics accessible to non-technical users.

### 6. GraalVM Native Image — A Brief Aside (1 min)

Acknowledge briefly:
- "We experimented with GraalVM native compilation for faster startup"
- Startup improved, but it didn't meaningfully change the user experience
- **The LLM API call dominates latency** (1-3 seconds) — shaving 500ms off JVM startup is noise
- Reflection-heavy Spring AI + MCP made native image configuration painful
- "For local developer tools, standard JVM is fine. Save native image for serverless or CLI tools where cold start matters."

### 7. Lessons Learned & What's Next (3 min)

**For the Spring developer audience**:

1. **MCP is the new integration standard** — like REST was for services, MCP is becoming the standard for AI tool integration. Learn it now.
2. **`@Tool` is the new `@GetMapping`** — same mental model: annotate a method, the framework exposes it via a protocol.
3. **Spring AI handles the plumbing** — both server and client sides have excellent auto-configuration. You focus on business logic.
4. **Osquery is underrated** — powerful, read-only, cross-platform system introspection that pairs perfectly with AI.
5. **Voice interfaces are table stakes soon** — the STT/TTS APIs are good enough now. Consider adding voice to your tools.

**What's next / where to explore**:
- Building your own MCP servers for your domain (databases, monitoring, deployment)
- Claude Desktop / IDE integration via MCP
- The skill-based approach as a lightweight alternative (mention the Claude Code skill in this project)

### 8. Q&A / Close (3 min)

- Point to the GitHub repos
- Mention the YouTube video for a longer walkthrough
- "Any questions?"

---

## Demo Contingency Plan

Live demos with voice recognition can fail. Have these fallbacks ready:

1. **Primary**: Live voice demo with actual microphone
2. **Fallback 1**: Type the query into the JavaFX client instead of speaking
3. **Fallback 2**: Run the Spring AI CLI client (`./gradlew run --args="\"system health\""`)
4. **Fallback 3**: Show the YouTube video clip of it working (have it cued up)

---

## Slides Needed (Minimal)

Since this is a code walkthrough, keep slides to a minimum:

1. **Title slide** — talk title, your name, social links
2. **Architecture diagram** — the voice → LLM → MCP → osquery flow (show during section 2)
3. **"What is MCP?"** — one slide: protocol overview, STDIO transport, tool registry
4. **"What is Osquery?"** — one slide: SQL for your OS, example query + output
5. **Key takeaways** — the 5 lessons learned (show during section 7)
6. **Links & resources** — GitHub repos, YouTube video, Spring AI docs, MCP spec

Everything else is live in the IDE.

---

## Code to Have Open in IDE (Pre-loaded tabs)

1. `OsqueryService.java` — the star of the show
2. `OsqueryMcpServerApplication.java` — the tiny main class
3. `application.properties` (server)
4. `SpringAiOsqueryClientApplication.java` — client walkthrough
5. `application.yml` (client)
6. Key files from starfleet-voice-interface (STT/TTS integration, main UI)
7. `build.gradle.kts` (both server and client) — for dependency discussion

---

## Timing Budget

| Section | Minutes | Cumulative |
|---------|---------|------------|
| Opening demo | 2 | 2 |
| Architecture overview | 3 | 5 |
| MCP Server walkthrough | 8 | 13 |
| Spring AI Client | 5 | 18 |
| Voice Interface (JavaFX) | 5 | 23 |
| GraalVM aside | 1 | 24 |
| Lessons & next steps | 3 | 27 |
| Q&A | 3 | 30 |

---

## Key Phrases / Sound Bites

- "MCP is JDBC for AI tools"
- "`@Tool` is the new `@GetMapping`"
- "SQL for your operating system"
- "The LLM is the brains, osquery is the eyes, MCP is the nervous system"
- "Configuration over code — zero lines of protocol handling"
- "Everything you just saw is Java"
