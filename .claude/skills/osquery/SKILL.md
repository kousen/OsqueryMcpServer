---
name: osquery
description: |
  System diagnostics using osquery. Use when asked about CPU usage, memory
  consumption, network connections, running processes, disk I/O, fan speeds,
  temperatures, or system security. Triggers: "why is my computer slow",
  "what's using memory", "what's using CPU", "network connections",
  "suspicious processes", "system health", "fan running", "overheating",
  "disk activity", "what processes are running".
allowed-tools: Bash
---

# Osquery System Diagnostics Skill

Use osquery to answer system diagnostic questions through natural language.

## How to Execute Queries

Run osquery with JSON output for structured data:

```bash
osqueryi --json "YOUR SQL QUERY HERE"
```

**Important**: Always use `--json` flag for parseable output.

## Quick Reference: Common Diagnostics

| User Question | Use This Query |
|---------------|----------------|
| "Why is my computer slow?" | High CPU or High Memory query |
| "What's using all my RAM?" | High Memory query |
| "What's using my network?" | Network Connections query |
| "Is my fan running hot?" | Temperature Info query |
| "Is my system compromised?" | Suspicious Processes query |
| "What's causing disk slowdown?" | High Disk I/O query |
| "Give me a system overview" | System Health Summary |

## Predefined Queries

See [queries.md](queries.md) for complete SQL templates with explanations.

## Interpreting Results

### CPU Usage
- **cpu_percent > 50%** for a single process is high
- System processes like `kernel_task`, `WindowServer` are normal consumers
- Browser processes (`Google Chrome Helper`) often top the list - normal

### Memory Usage
- **resident_mb > 1000** (1GB) is significant
- Compare to total system RAM (check with `SELECT physical_memory FROM system_info`)
- Multiple instances of same app (e.g., Chrome tabs) add up

### Network Connections
- `ESTABLISHED` = active connection
- `LISTEN` = waiting for connections (servers)
- Unexpected connections to unknown IPs warrant investigation

### Temperature (macOS)
- CPU temps **under 80C** are normal under load
- **Above 90C** sustained indicates cooling issues
- Fan speeds increase automatically with temperature

### Suspicious Processes
Flagged processes aren't necessarily malware. Common false positives:
- **"No parent process"**: Daemons that outlive their parent are normal
- **"Running from temp"**: Installers and updaters often run from /tmp
- Investigate further with: `SELECT * FROM processes WHERE pid = <pid>`

## Platform Notes

**macOS-specific tables**:
- `temperature_sensors`, `fan_speed_sensors` - Only on macOS
- `launchd` - macOS service manager

**Linux alternatives**:
- Use `/proc` filesystem tables
- `systemd_units` instead of `launchd`

## Error Handling

If you see "no such table", the table may not exist on this platform:
```bash
# List available tables
osqueryi --json "SELECT name FROM osquery_registry WHERE active = 1"
```

## Follow-up Suggestions

After showing results, consider suggesting:
- "Would you like me to investigate any specific process?"
- "Should I check what files this process has open?"
- "Want me to look at the network activity for this process?"
