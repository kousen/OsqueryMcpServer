package com.kousenit.osquerymcpserver;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class OsqueryService {
    private static final Logger logger = LoggerFactory.getLogger(OsqueryService.class);
    
    @PostConstruct
    public void checkOsqueryAvailable() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"osqueryi", "--version"});
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                String version = new String(p.getInputStream().readAllBytes()).trim();
                logger.info("Osquery is available: {}", version);
            } else {
                logger.error("Osquery check failed with exit code: {}", exitCode);
            }
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
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"osqueryi", "--json", sql}
            );
            String result = new String(p.getInputStream().readAllBytes());
            logger.debug("Query completed successfully, returned {} bytes", result.length());
            return result;
        } catch (Exception e) {
            logger.error("Failed to execute osquery: {}", sql, e);
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
        return executeOsquery(String.format("PRAGMA table_info(%s)", tableName));
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
               SELECT name, pid, uid, cpu_time FROM processes ORDER BY cpu_time DESC LIMIT 10
            
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
               SELECT name, label, program, state FROM launchd WHERE state = 'running'
            
            8. User sessions:
               SELECT user, host, time FROM logged_in_users
            """;
    }

    @Tool(description = """
         Run a predefined query for high CPU usage processes.
         Returns the top 10 processes consuming the most CPU time.
         Useful for answering 'Why is my computer slow?' or 'What's using CPU?'""")
    public String getHighCpuProcesses() {
        return executeOsquery(
            "SELECT name, pid, uid, cpu_time, " +
            "ROUND((cpu_time * 100.0 / (SELECT SUM(cpu_time) FROM processes)), 2) AS cpu_percent " +
            "FROM processes ORDER BY cpu_time DESC LIMIT 10"
        );
    }

    @Tool(description = """
         Run a predefined query for high memory usage processes.
         Returns the top 10 processes consuming the most memory.
         Useful for answering 'What's using all my RAM?' or 'Why is memory full?'""")
    public String getHighMemoryProcesses() {
        return executeOsquery(
            "SELECT name, pid, uid, " +
            "ROUND(resident_size / 1024.0 / 1024.0, 2) AS resident_mb, " +
            "ROUND(total_size / 1024.0 / 1024.0, 2) AS total_mb " +
            "FROM processes ORDER BY resident_size DESC LIMIT 10"
        );
    }

    @Tool(description = """
         Get current network connections.
         Shows active network connections with process information.
         Useful for 'What's connected to the internet?' or 'What's using my network?'""")
    public String getNetworkConnections() {
        return executeOsquery(
            "SELECT DISTINCT p.name, p.pid, pos.local_address, pos.local_port, " +
            "pos.remote_address, pos.remote_port, pos.state " +
            "FROM process_open_sockets pos " +
            "JOIN processes p ON pos.pid = p.pid " +
            "WHERE pos.state = 'ESTABLISHED' OR pos.state = 'LISTEN'"
        );
    }

    @Tool(description = """
         Get system temperature and fan information (macOS only).
         Shows temperature sensors and fan speeds.
         Useful for 'Why is my fan running?' or 'Is my computer overheating?'""")
    public String getTemperatureInfo() {
        // This combines temperature and fan data
        String temps = executeOsquery("SELECT name, celsius FROM temperature_sensors");
        String fans = executeOsquery("SELECT name, actual_speed, min_speed, max_speed FROM fan_control_sensors");
        return String.format("Temperature sensors:\n%s\n\nFan speeds:\n%s", temps, fans);
    }
}
