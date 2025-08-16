package com.kousenit.osquerymcpserver;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OsqueryService {
    private static final Logger logger = LoggerFactory.getLogger(OsqueryService.class);
    
    @PostConstruct
    public void checkOsqueryAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("osqueryi", "--version");
            Process p = pb.start();
            
            boolean completed = p.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                p.destroyForcibly();
                logger.error("Osquery version check timed out after 5 seconds");
                return;
            }
            int exitCode = p.exitValue();
            if (exitCode == 0) {
                String version;
                try (var reader = p.inputReader()) {
                    version = reader.lines().collect(Collectors.joining("\n")).trim();
                }
                logger.info("Osquery is available: {}", version);
            } else {
                logger.error("Osquery check failed with exit code: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Osquery version check was interrupted", e);
        } catch (Exception e) {
            logger.error("Osquery is not available. Please install osquery: https://osquery.io/downloads/", e);
        }
    }

    @Tool(description = """
         Execute osquery SQL queries to inspect system state.
         Query processes, users, network connections, and other OS data.
         Example: SELECT name, pid FROM processes""")
    public String executeOsquery(String sql) {
        logger.debug("Executing osquery SQL: {}", sql);
        long startTime = System.currentTimeMillis();
        
        try {
            ProcessBuilder pb = new ProcessBuilder("osqueryi", "--json", sql);
            Process p = pb.start();
            
            // Read the output
            String result;
            try (var reader = p.inputReader()) {
                result = reader.lines().collect(Collectors.joining("\n"));
            }
            
            // Wait for process to complete with timeout
            boolean completed = p.waitFor(30, TimeUnit.SECONDS);
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (!completed) {
                p.destroyForcibly();
                logger.error("Query timed out after 30 seconds: {}", sql);
                return "Error: Query execution timed out after 30 seconds";
            }
            
            int exitCode = p.exitValue();
            
            if (exitCode != 0) {
                // Read error stream if command failed
                String errorOutput;
                try (var errorReader = p.errorReader()) {
                    errorOutput = errorReader.lines().collect(Collectors.joining("\n"));
                }
                logger.error("Osquery exited with code {} after {}ms. Error: {}", exitCode, executionTime, errorOutput);
                return "Error: " + errorOutput;
            }
            
            logger.debug("Query completed successfully in {}ms, returned {} bytes", executionTime, result.length());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Query interrupted after {}ms: {}", executionTime, sql, e);
            return "Error: Query execution was interrupted";
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to execute osquery after {}ms: {}", executionTime, sql, e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "List available osquery tables on this system")
    public String listOsqueryTables() {
        return executeOsquery(
                "SELECT name FROM osquery_registry WHERE active = 1");
    }

    @Tool(description = """
         Get schema information for a specific osquery table.
         Shows column names and types to help construct queries.
         Example: getTableSchema("processes")""")
    public String getTableSchema(String tableName) {
        logger.debug("Getting schema for table: {}", tableName);
        return executeOsquery("PRAGMA table_info(%s)".formatted(tableName));
    }

    @Tool(description = """
         Get common queries for system diagnostics.
         Returns helpful example queries for common troubleshooting scenarios like:
         - High CPU usage processes
         - Memory consumption
         - Network connections
         - Recently modified files
         - User login history""")
    public String getCommonQueries() {
        return """
            Common diagnostic queries:
            
            1. Top CPU consuming processes:
               SELECT name, pid, uid, (user_time + system_time) AS cpu_time FROM processes ORDER BY cpu_time DESC LIMIT 10
            
            2. Memory usage by process:
               SELECT name, pid, resident_size, total_size FROM processes ORDER BY resident_size DESC LIMIT 10
            
            3. Network connections:
               SELECT pid, local_address, local_port, remote_address, remote_port, state FROM process_open_sockets WHERE state = 'ESTABLISHED'
            
            4. Recently modified files in home directory:
               SELECT path, mtime, size FROM file WHERE path LIKE '/Users/%' AND mtime > (strftime('%s', 'now') - 3600)
            
            5. System information:
               SELECT hostname, cpu_brand, physical_memory, hardware_vendor, hardware_model FROM system_info
            
            6. Disk usage:
               SELECT path, blocks_available, blocks, inodes_free FROM mounts WHERE path = '/'
            
            7. Running services (macOS):
               SELECT name, label, program, disabled FROM launchd WHERE disabled = '0'
            
            8. User sessions:
               SELECT user, host, time FROM logged_in_users
            """;
    }

    @Tool(description = """
         Run a predefined query for high CPU usage processes.
         Returns the top 10 processes consuming the most CPU time.
         Useful for answering 'Why is my computer slow?' or 'What's using CPU?'""")
    public String getHighCpuProcesses() {
        return executeOsquery("""
            SELECT name, pid, uid, (user_time + system_time) AS cpu_time,
            ROUND(((user_time + system_time) * 100.0 / (SELECT SUM(user_time + system_time) FROM processes)), 2) AS cpu_percent
            FROM processes ORDER BY cpu_time DESC LIMIT 10
            """);
    }

    @Tool(description = """
         Run a predefined query for high memory usage processes.
         Returns the top 10 processes consuming the most memory.
         Useful for answering 'What's using all my RAM?' or 'Why is memory full?'""")
    public String getHighMemoryProcesses() {
        return executeOsquery("""
            SELECT name, pid, uid,
            ROUND(resident_size / 1024.0 / 1024.0, 2) AS resident_mb,
            ROUND(total_size / 1024.0 / 1024.0, 2) AS total_mb
            FROM processes ORDER BY resident_size DESC LIMIT 10
            """);
    }

    @Tool(description = """
         Get current network connections.
         Shows active network connections with process information.
         Useful for 'What's connected to the internet?' or 'What's using my network?'""")
    public String getNetworkConnections() {
        return executeOsquery("""
            SELECT DISTINCT p.name, p.pid, pos.local_address, pos.local_port,
            pos.remote_address, pos.remote_port, pos.state
            FROM process_open_sockets pos
            JOIN processes p ON pos.pid = p.pid
            WHERE pos.state = 'ESTABLISHED' OR pos.state = 'LISTEN'
            """);
    }

    @Tool(description = """
         Get system temperature and fan information (macOS only).
         Shows temperature sensors and fan speeds.
         Useful for 'Why is my fan running?' or 'Is my computer overheating?'""")
    public String getTemperatureInfo() {
        // This combines temperature and fan data
        String temps = executeOsquery("SELECT name, celsius FROM temperature_sensors");
        String fans = executeOsquery("SELECT fan, name, actual, min, max FROM fan_speed_sensors");
        
        // Handle cases where tables don't exist (non-macOS or missing sensors)
        if (temps.startsWith("Error:") && fans.startsWith("Error:")) {
            return "Temperature and fan information not available on this system";
        }
        
        StringBuilder result = new StringBuilder();
        
        if (!temps.startsWith("Error:")) {
            result.append("Temperature sensors:\n").append(temps);
        } else {
            result.append("Temperature sensors: Not available");
        }
        
        result.append("\n\n");
        
        if (!fans.startsWith("Error:")) {
            result.append("Fan speeds:\n").append(fans);
        } else {
            result.append("Fan speeds: Not available");
        }
        
        return result.toString();
    }

    @Tool(description = "Get overall system health summary")
    public String getSystemHealthSummary() {
        // Combine CPU, memory, disk, network data
        String cpu = getHighCpuProcesses();
        String memory = getHighMemoryProcesses();
        String disk = executeOsquery(
                "SELECT path, blocks_available, blocks, inodes_free FROM mounts WHERE path = '/'");
        String network = getNetworkConnections();
        String temperature = getTemperatureInfo();
        return """
            System Health Summary:
            
            CPU Usage:
            %s
            
            Memory Usage:
            %s
            
            Disk Usage:
            %s
            
            Network Connections:
            %s
            
            Temperature and Fans:
            %s
            """.formatted(cpu, memory, disk, network, temperature);
    }

    @Tool(description = """
         Identify suspicious processes that may indicate security issues or malware.
         Returns processes with unusual characteristics like no parent process,
         unusual network activity, or processes running from unusual locations.
         Useful for answering 'Is my system compromised?' or 'What looks suspicious?'""")
    public String getSuspiciousProcesses() {
        return executeOsquery("""
            SELECT p.name, p.pid, p.ppid, p.uid, p.path,
            CASE
                WHEN p.ppid = 0 AND p.pid != 1 THEN 'No parent process'
                WHEN p.path LIKE '/tmp/%' OR p.path LIKE '/var/tmp/%' THEN 'Running from temp directory'
                WHEN p.name != SUBSTR(p.path, LENGTH(p.path) - LENGTH(p.name) + 1) THEN 'Process name mismatch'
                ELSE 'Normal'
            END as suspicious_reason,
            (p.user_time + p.system_time) as cpu_time
            FROM processes p
            WHERE p.ppid = 0 AND p.pid != 1
               OR p.path LIKE '/tmp/%' 
               OR p.path LIKE '/var/tmp/%'
               OR p.name != SUBSTR(p.path, LENGTH(p.path) - LENGTH(p.name) + 1)
            ORDER BY cpu_time DESC
            LIMIT 20
            """);
    }

    @Tool(description = """
         Get processes with high disk I/O activity that may be causing system slowdowns.
         Returns processes sorted by disk read/write operations.
         Useful for answering 'Why is my disk so busy?' or 'What's causing disk slowdown?'""")
    public String getHighDiskIOProcesses() {
        return executeOsquery("""
            SELECT p.name, p.pid, p.uid,
            ROUND(p.disk_bytes_read / 1024.0 / 1024.0, 2) AS disk_read_mb,
            ROUND(p.disk_bytes_written / 1024.0 / 1024.0, 2) AS disk_write_mb,
            ROUND((p.disk_bytes_read + p.disk_bytes_written) / 1024.0 / 1024.0, 2) AS total_disk_mb,
            p.path
            FROM processes p
            WHERE p.disk_bytes_read > 0 OR p.disk_bytes_written > 0
            ORDER BY (p.disk_bytes_read + p.disk_bytes_written) DESC
            LIMIT 15
            """);
    }
}
