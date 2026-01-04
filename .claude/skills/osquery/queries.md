# Osquery Query Templates

## High CPU Processes

**Triggers**: "slow computer", "CPU usage", "what's using CPU", "laggy"

```sql
SELECT name, pid, uid, (user_time + system_time) AS cpu_time,
ROUND(((user_time + system_time) * 100.0 / (SELECT SUM(user_time + system_time) FROM processes)), 2) AS cpu_percent
FROM processes ORDER BY cpu_time DESC LIMIT 10
```

**Baseline guidance**:
- Single process > 50% CPU sustained = investigate
- `kernel_task`, `WindowServer` (macOS) normally high
- Browser helpers consuming CPU often = too many tabs or heavy pages
- `mds`, `mdworker` (Spotlight indexing) spikes are temporary

---

## High Memory Processes

**Triggers**: "memory usage", "RAM", "what's eating memory", "out of memory"

```sql
SELECT name, pid, uid,
ROUND(resident_size / 1024.0 / 1024.0, 2) AS resident_mb,
ROUND(total_size / 1024.0 / 1024.0, 2) AS total_mb
FROM processes ORDER BY resident_size DESC LIMIT 10
```

**Baseline guidance**:
- Check total RAM: `SELECT physical_memory FROM system_info`
- Process using > 25% of total RAM = significant
- Browsers, IDEs, Docker commonly use 1-4GB each
- `kernel_task` memory is normal and managed by macOS

---

## Network Connections

**Triggers**: "network connections", "what's using internet", "connected to", "network activity"

```sql
SELECT DISTINCT p.name, p.pid, pos.local_address, pos.local_port,
pos.remote_address, pos.remote_port, pos.state
FROM process_open_sockets pos
JOIN processes p ON pos.pid = p.pid
WHERE pos.state = 'ESTABLISHED' OR pos.state = 'LISTEN'
```

**Interpreting states**:
- `ESTABLISHED` = active bidirectional connection
- `LISTEN` = server waiting for connections
- `TIME_WAIT` = connection closing (normal cleanup)

**Common legitimate connections**:
- `cloudd`, `apsd` = Apple cloud services
- `Dropbox`, `OneDrive` = file sync
- Browsers to ports 443 (HTTPS), 80 (HTTP)
- `ssh`, `sshd` on port 22

---

## Temperature and Fan Info (macOS)

**Triggers**: "fan running", "overheating", "temperature", "hot computer", "thermal"

```sql
-- Temperature sensors
SELECT name, celsius FROM temperature_sensors

-- Fan speeds
SELECT fan, name, actual, min, max FROM fan_speed_sensors
```

**Baseline guidance**:
- **Idle CPU**: 40-60C is normal
- **Under load**: 70-85C is acceptable
- **> 90C sustained**: Cooling problem, check vents/fans
- Fan RPM increases automatically with temperature
- High fan + normal temp = cooling working correctly
- High fan + high temp = heavy workload or blocked airflow

---

## Suspicious Processes

**Triggers**: "suspicious", "malware", "security check", "compromised", "virus"

```sql
SELECT p.name, p.pid, p.parent, p.uid, p.path,
CASE
    WHEN p.parent = 0 AND p.pid != 1 THEN 'No parent process'
    WHEN p.path LIKE '/tmp/%' OR p.path LIKE '/var/tmp/%' THEN 'Running from temp directory'
    WHEN p.on_disk = 0 THEN 'Not on disk'
    ELSE 'Flagged'
END as suspicious_reason,
(p.user_time + p.system_time) as cpu_time
FROM processes p
WHERE (p.parent = 0 AND p.pid != 1)
   OR p.path LIKE '/tmp/%'
   OR p.path LIKE '/var/tmp/%'
   OR p.on_disk = 0
ORDER BY cpu_time DESC
LIMIT 20
```

**Understanding the flags**:
- **No parent process**: Process outlived parent. Common for daemons, but could indicate orphaned malware.
- **Running from temp**: Installers, updaters run from /tmp. Persistence from temp is suspicious.
- **Not on disk**: Binary no longer exists on filesystem. Could be deleted malware or staged Apple services (often false positive).

**Follow-up investigation**:
```sql
-- Check what files a process has open
SELECT path FROM process_open_files WHERE pid = <pid>

-- Check network connections for a process
SELECT * FROM process_open_sockets WHERE pid = <pid>

-- Get detailed process info
SELECT * FROM processes WHERE pid = <pid>
```

---

## High Disk I/O Processes

**Triggers**: "disk activity", "disk slow", "hard drive busy", "disk usage"

```sql
SELECT p.name, p.pid, p.uid,
ROUND(p.disk_bytes_read / 1024.0 / 1024.0, 2) AS disk_read_mb,
ROUND(p.disk_bytes_written / 1024.0 / 1024.0, 2) AS disk_write_mb,
ROUND((p.disk_bytes_read + p.disk_bytes_written) / 1024.0 / 1024.0, 2) AS total_disk_mb,
p.path
FROM processes p
WHERE p.disk_bytes_read > 0 OR p.disk_bytes_written > 0
ORDER BY (p.disk_bytes_read + p.disk_bytes_written) DESC
LIMIT 15
```

**Common high-I/O processes**:
- `mds`, `mdworker` = Spotlight indexing (temporary)
- `Time Machine` = backup in progress
- `fsck` = disk repair
- IDEs indexing large projects

---

## System Health Summary

**Triggers**: "system health", "overview", "status", "how's my system"

Run multiple queries and combine:
1. High CPU processes
2. High Memory processes
3. Disk usage: `SELECT path, blocks_available, blocks, inodes_free FROM mounts WHERE path = '/'`
4. Network connections
5. Temperature and fans (macOS)

---

## Utility Queries

### List Available Tables
```sql
SELECT name FROM osquery_registry WHERE active = 1
```

### Get Table Schema
```sql
PRAGMA table_info(TABLE_NAME)
```

### System Information
```sql
SELECT hostname, cpu_brand, physical_memory, hardware_vendor, hardware_model FROM system_info
```

### Disk Space
```sql
SELECT path, ROUND(blocks_available * blocks_size / 1024.0 / 1024.0 / 1024.0, 2) AS free_gb,
ROUND(blocks * blocks_size / 1024.0 / 1024.0 / 1024.0, 2) AS total_gb
FROM mounts WHERE path = '/'
```

### Logged In Users
```sql
SELECT user, host, time FROM logged_in_users
```

### Recently Modified Files (last hour)
```sql
SELECT path, mtime, size FROM file
WHERE path LIKE '/Users/%' AND mtime > (strftime('%s', 'now') - 3600)
```

### Running Services (macOS)
```sql
SELECT name, label, program, disabled FROM launchd WHERE disabled = '0'
```

---

## Find Application Processes

**Triggers**: "is X running", "find app", "what sessions", "Claude processes", "Docker processes"

```sql
-- Generic pattern: replace APP_NAME with the application
SELECT name, pid, path, cmdline
FROM processes
WHERE name LIKE '%APP_NAME%'
   OR cmdline LIKE '%APP_NAME%'
   OR path LIKE '%APP_NAME%'
```

**Common applications**:
```sql
-- Claude (Desktop + Code CLI)
SELECT name, pid, path, cmdline FROM processes
WHERE name LIKE '%claude%' OR cmdline LIKE '%claude%' OR path LIKE '%claude%'

-- Docker
SELECT name, pid, path FROM processes WHERE name LIKE '%docker%'

-- Node.js applications
SELECT name, pid, cmdline FROM processes WHERE name = 'node'

-- Python scripts
SELECT name, pid, cmdline FROM processes WHERE name LIKE '%python%' OR name LIKE '%Python%'
```

---

## Debugging: Check Table Schema

**Triggers**: "what columns", "table schema", "column names"

When a query fails with "no such column", check the actual schema:

```sql
PRAGMA table_info(TABLE_NAME)
```

Example output shows column names and types available.

---

## Example Queries for Common Scenarios

| Scenario | Query |
|----------|-------|
| "What Chrome tabs are using memory?" | `SELECT name, pid, resident_size FROM processes WHERE name LIKE '%Chrome%' ORDER BY resident_size DESC` |
| "Is Docker running?" | `SELECT name, pid, state FROM processes WHERE name LIKE '%docker%'` |
| "What's listening on port 8080?" | `SELECT p.name, pos.* FROM process_open_sockets pos JOIN processes p ON pos.pid = p.pid WHERE pos.local_port = 8080` |
| "Show me Java processes" | `SELECT name, pid, path, cmdline FROM processes WHERE name LIKE '%java%'` |
| "What started in the last 10 minutes?" | `SELECT name, pid, start_time FROM processes WHERE start_time > (strftime('%s', 'now') - 600) ORDER BY start_time DESC` |
| "Find Claude sessions" | `SELECT name, pid, cmdline FROM processes WHERE cmdline LIKE '%claude%'` |
