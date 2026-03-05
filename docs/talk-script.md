# Talk Script: "Computer, Run a Level 1 Diagnostic"
## Building a Star Trek Computer with Java, Spring AI, and MCP

**Duration**: 30 minutes
**Audience**: Java developers at a Virtual AI Day summit

---

## 1. THE HOOK — Open with the Demo (2 min)

No preamble. No slides. Just open the Starfleet Voice Interface app.

> "I want to start by showing you something."

Press the button, say: **"Computer, run a level 1 diagnostic."**

Let it work. Let the audience watch the tool calls appear, the streaming text fill in. Don't explain anything yet.

> "That was a JavaFX app, talking to a Spring Boot MCP server, which used osquery to inspect every aspect of my operating system — CPU, memory, disk, network, temperature — all from a voice command. Let me show you how simple this actually is."

---

## 2. WHAT JUST HAPPENED? — The Sequence (5 min)

**[SHOW: talk-diagram-sequence.mmd]**

Walk through the diagram backwards from the result:

> "What you just saw was a chain of translations. My voice became text via Whisper AI. That text became a prompt sent to an LLM. The LLM recognized it needed a tool — specifically `getSystemHealthSummary` — and called my MCP server. The server fired off 5 osquery calls in parallel using virtual threads, assembled the results, and sent them back. The LLM then synthesized a natural language summary, and streamed it back to the UI line by line."

Key point to emphasize:

> "Every piece of this is simple. The interesting part is how they compose together."

---

## 3. THE PROGRESSION — From CLI to Voice (8 min)

**[SHOW: talk-diagram-progression.mmd]**

### 3a. Layer 1: osquery itself (1 min)

> "If you haven't seen osquery before, it's an open-source tool from Meta that exposes your entire operating system as SQL tables. Processes, network connections, fan speeds, temperature sensors — all queryable with SELECT statements."

> "The catch? You have to know the tables, the columns, and the SQLite dialect. To find out why your fan is loud, you need to write something like `SELECT fan, name, actual FROM fan_speed_sensors`. That's powerful, but it's not exactly user-friendly."

### 3b. Layer 2: Wrap it in a Skill (2 min)

**[SHOW: .claude/skills/osquery/SKILL.md — briefly scroll through it]**

> "The first thing I did was create a Claude Code skill. This is just a markdown file that teaches the AI how to use osquery. It maps natural language questions to SQL templates."

> "With this skill, I can type `/osquery tell me about my memory usage` in Claude Code, and it writes the SQL, runs osqueryi, and interprets the results."

> "Skills are great — but they're proprietary to tools that understand them. Claude Code knows what a skill is. Your IDE's AI assistant probably doesn't. Your custom app definitely doesn't."

### 3c. Layer 3: MCP Server (3 min)

> "This is where MCP changes the game. By wrapping the same capability as an MCP server, it becomes discoverable by any MCP client — Claude Desktop, VS Code Copilot, Cursor, IntelliJ, or your own application."

> "Think of MCP as what REST was for web services. It's a standard protocol that lets AI tools discover and call your capabilities without knowing anything about your implementation."

**[SHOW: OsqueryService.java:48-57 — the executeOsquery @Tool annotation]**

> "Here's the core. The `@Tool` annotation from Spring AI marks this method as an MCP tool. The description tells the LLM what it does. The method takes SQL, shells out to osqueryi with a ProcessBuilder, and returns the JSON result. That's the entire server, conceptually."

### 3d. Layer 4: Voice Client (2 min)

> "The voice client is a separate Spring Boot app using JavaFX. It records audio, sends it to Whisper for transcription, then sends that text through the MCP protocol to our server."

> "Notice these are different Java versions. The server runs Java 25 with all the latest features. The client runs Java 21 because that's what JavaFX needs. MCP doesn't care — it's just a protocol."

---

## 4. THE JAVA BITS THAT MATTER (8 min)

### 4a. Why multiple tools instead of one? (2 min)

**[SHOW: talk-diagram-tool-matching.mmd]**

> "I could have exposed just `executeOsquery(sql)` and let the LLM figure out the SQL every time. But that puts all the burden on the LLM."

**[SHOW: OsqueryService.java — scroll through the method list, pointing out method names]**

- `getHighCpuProcesses()` — line 159
- `getHighMemoryProcesses()` — line 171
- `getNetworkConnections()` — line 184
- `getTemperatureInfo()` — line 198
- `getSystemHealthSummary()` — line 234
- `getSuspiciousProcesses()` — line 277
- `getHighDiskIOProcesses()` — line 301

> "Instead, I created 11 tools with descriptive names and descriptions. When you say 'why is my fan running hot?', the LLM doesn't need to know osquery SQL. It just matches your question to `getTemperatureInfo()` based on the description: *'System temperature and fan speeds. Useful for: Why is my fan running? Is my computer overheating?'*"

> "Think of it as indexing. Each tool is a well-labeled entry point. The more specific your tools, the more reliably the LLM picks the right one. The generic `executeOsquery` is still there as a fallback for anything the specialized tools don't cover."

### 4b. @Tool annotation — Spring AI's secret weapon (2 min)

**[SHOW: OsqueryService.java:48-52]**

```java
@Tool(description = """
     Execute osquery SQL queries to inspect system state.
     Query processes, users, network connections, and other OS data.
     Example: SELECT name, pid FROM processes""")
public String executeOsquery(String sql) {
```

> "This is the entire MCP integration. One annotation. Spring AI's MCP server starter scans for `@Tool` methods, registers them with the MCP protocol, handles JSON-RPC serialization, tool discovery, everything. You write a method, annotate it, and it's an MCP tool."

> "The description is doing real work here. It's not documentation for humans — it's the contract with the LLM. Write it like you're explaining to a very capable intern what this method is for and when to use it."

### 4c. Virtual threads — parallel system queries (2 min)

**[SHOW: OsqueryService.java:234-269 — getSystemHealthSummary()]**

> "When you ask for a full system diagnostic, I need CPU, memory, disk, network, and temperature data. These are 5 independent osquery calls. Running them sequentially would take 5x as long."

**[HIGHLIGHT: line 235]**
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
```

> "Virtual threads from Project Loom. One line gives me a lightweight executor. Each query runs on its own virtual thread."

**[HIGHLIGHT: lines 236-244]**

> "Five CompletableFuture.supplyAsync calls, all dispatched to the virtual thread executor. Each one shells out to osqueryi independently."

**[HIGHLIGHT: lines 263-268]**

> "Then I join them all with a text block. The `.join()` calls block until each result is ready, but since we're on virtual threads, that blocking is essentially free."

> "No thread pool tuning. No reactive complexity. Just straightforward concurrent code that reads like sequential code. That's the virtual threads promise, and it delivers here."

### 4d. ProcessBuilder — it's just shelling out (2 min)

**[SHOW: OsqueryService.java:56-68]**

```java
ProcessBuilder pb = new ProcessBuilder("osqueryi", "--json", sql);
Process p = pb.start();
```

> "I want to be transparent about something: this entire server is just a fancy wrapper around a command-line tool. ProcessBuilder, three arguments, read the output, return it. That's the core."

**[HIGHLIGHT: lines 67-68 — timeout handling]**

> "The only real engineering is defensive: a 30-second timeout so a bad query doesn't hang the server, proper error stream capture, and InterruptedException handling."

> "Sometimes the simplest architecture is the right one. Don't let the Spring AI and MCP layers fool you — underneath, it's just invoking `osqueryi --json` and returning what comes back."

---

## 5. THE CLIENT SIDE — Brief Tour (3 min)

**[SHOW: McpClientService.java:22-25 — constructor injection]**

```java
public McpClientService(OpenAiChatModel chatModel, SyncMcpToolCallbackProvider toolCallbackProvider) {
```

> "The client gets two things injected: a chat model and an MCP tool callback provider. Spring AI auto-discovers the tools from the server — all 11 of them — through MCP's tool discovery protocol."

**[SHOW: McpClientService.java:34-61 — processCommand method]**

> "The system prompt sets the Star Trek character. The `.stream().content()` gives us a Flux — reactive streaming. Each chunk of text flows back to the JavaFX UI as it's generated."

**[SHOW: McpClientService.java:36-56 — system prompt]**

> "Notice this: *'If a command is outside your capabilities — weapons, navigation, shields — respond in character.'* So when I said 'arm phasers', it stayed in character and explained it couldn't do that. That's just prompt engineering, but it makes the demo fun."

**[SHOW: VoiceController.java:86-115 — the recording flow]**

> "Press button, record, release, transcribe, send to MCP, stream back results. The JavaFX Platform.runLater calls keep the UI responsive. Nothing groundbreaking, but it's satisfying that the entire pipeline — microphone to operating system and back — is Java."

---

## 6. LESSONS AND TAKEAWAYS (4 min)

### The pattern generalizes

> "Forget osquery for a moment. The pattern here works for anything:"
> - "Got an internal CLI tool? Wrap it in `@Tool` methods with good descriptions."
> - "Got a REST API that's hard to use? Same thing."
> - "Got a database with complex queries? Pre-build the common ones as tools."
>
> "You're building a natural language interface to existing capabilities. The MCP protocol makes it universally accessible."

### Tool design matters more than implementation

> "I spent more time writing tool descriptions than writing code. The description on `getTemperatureInfo()` — *'Why is my fan running? Is my computer overheating?'* — that's what makes the LLM pick the right tool. If I'd just written *'Get temperature data'*, it might miss the connection to a user asking about fan noise."

### You don't need bleeding edge everywhere

> "The server uses Java 25 because I wanted virtual threads and text blocks. The client uses Java 21 because JavaFX needs it. Spring AI 2 is a milestone release because that's what works with Spring Boot 4. Use what fits."

### MCP is the universal translator

> "A skill works in Claude Code. An MCP server works everywhere — Claude Desktop, VS Code, IntelliJ, Cursor, or your own JavaFX app with a Star Trek theme. Write it once, connect it anywhere."

---

## 7. CLOSE (1 min)

> "I started this project because I saw a great blog post by Daniela Petruzalek about building a USS Enterprise computer in Python. I wanted to do it in Java, and once I started, I kept finding that each layer — osquery, Spring AI, MCP, virtual threads, JavaFX — snapped together more easily than I expected."

> "The code is all on GitHub. The server is about 300 lines. The client is about the same. If you've got 30 minutes after this talk, you could have your own MCP server wrapping your favorite command-line tool."

> "Thank you."

---

## Quick Reference: Code Locations for Live Navigation

| What to show | File | Lines |
|---|---|---|
| @Tool annotation + description | `OsqueryService.java` | 48-52 |
| executeOsquery core logic | `OsqueryService.java` | 56-68 |
| ProcessBuilder invocation | `OsqueryService.java` | 57 |
| Timeout handling | `OsqueryService.java` | 67-74 |
| Virtual thread executor | `OsqueryService.java` | 235 |
| 5 parallel CompletableFutures | `OsqueryService.java` | 236-244 |
| Text block assembly + join | `OsqueryService.java` | 246-268 |
| Temperature parallel queries | `OsqueryService.java` | 199-203 |
| Tool descriptions (browse several) | `OsqueryService.java` | 108, 117, 155, 167, 194, 233, 272, 297 |
| Skill definition | `.claude/skills/osquery/SKILL.md` | 1-14 |
| Client MCP injection | `McpClientService.java` | 22-25 |
| System prompt (Star Trek) | `McpClientService.java` | 36-56 |
| Streaming + Flux | `McpClientService.java` | 57-61 |
| Voice recording flow | `VoiceController.java` | 86-115 |

## Diagram Files

| Diagram | File | When to show |
|---|---|---|
| Full request/response sequence | `docs/talk-diagram-sequence.mmd` | Section 2: "What just happened?" |
| Progression from CLI to voice | `docs/talk-diagram-progression.mmd` | Section 3: "The Progression" |
| Tool matching / LLM selection | `docs/talk-diagram-tool-matching.mmd` | Section 4a: "Why multiple tools?" |
