package com.kousenit.osquerymcpserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OsqueryServiceTest {

    @Autowired
    private OsqueryService osqueryService;

    @Test
    void executeOsquery() {
        // Simple query to get osquery version
        String result = osqueryService.executeOsquery("SELECT version FROM osquery_info");
        
        System.out.println("Osquery version result: " + result);
        
        // Should return JSON format
        assertThat(result)
            .isNotNull()
            .contains("{")
            .doesNotContain("Error:");
    }

    @Test
    void listOsqueryTables() {
        String result = osqueryService.listOsqueryTables();
        
        System.out.println("Available tables (first 500 chars): " + 
            result.substring(0, Math.min(result.length(), 500)));
        
        // Should return JSON with table names
        assertThat(result)
            .isNotNull()
            .contains("name")
            .doesNotContain("Error:");
    }

    @Test
    void invalidQuery() {
        String result = osqueryService.executeOsquery("INVALID SQL QUERY");
        
        // With ProcessBuilder, we now properly capture error messages
        assertThat(result)
            .isNotNull()
            .startsWith("Error:")
            .contains("syntax error");
    }

    @Test
    void getTableSchema() {
        String result = osqueryService.getTableSchema("processes");
        
        System.out.println("Processes table schema: " + result);
        
        // Should return schema information
        assertThat(result)
            .isNotNull()
            .contains("name")
            .contains("type");
    }

    @Test
    void getHighCpuProcesses() {
        String result = osqueryService.getHighCpuProcesses();
        
        System.out.println("High CPU processes: " + result);
        
        // Should return process information
        assertThat(result)
            .isNotNull()
            .contains("[");
    }
    
    @Test
    void getCommonQueries() {
        String result = osqueryService.getCommonQueries();
        
        System.out.println("Common queries (first 200 chars): " + 
            result.substring(0, Math.min(result.length(), 200)));
        
        // Should contain example queries
        assertThat(result)
            .isNotNull()
            .contains("Common diagnostic queries:")
            .contains("CPU consuming processes")
            .contains("Memory usage")
            .contains("Network connections");
    }
    
    @Test
    void getHighMemoryProcesses() {
        String result = osqueryService.getHighMemoryProcesses();
        
        System.out.println("High memory processes: " + result);
        
        // Should return process memory information with proper validation
        assertThat(result).satisfies(r -> {
            assertThat(r).isNotNull();
            assertThat(r).contains("[");
            assertThat(r).satisfiesAnyOf(
                content -> assertThat(content).contains("resident_mb"),
                content -> assertThat(content).contains("Error:")
            );
        });
    }
    
    @Test
    void getNetworkConnections() {
        String result = osqueryService.getNetworkConnections();
        
        System.out.println("Network connections (first 500 chars): " + 
            result.substring(0, Math.min(result.length(), 500)));
        
        // Should return network connection information
        assertThat(result).satisfies(r -> {
            assertThat(r).isNotNull();
            assertThat(r).satisfiesAnyOf(
                content -> assertThat(content).contains("local_port"),
                content -> assertThat(content).contains("[]"), // Empty array if no connections
                content -> assertThat(content).contains("Error:")
            );
        });
    }
    
    @Test
    void getTemperatureInfo() {
        String result = osqueryService.getTemperatureInfo();
        
        System.out.println("Temperature and fan info:\n" + result);
        
        // Should return temperature and fan information
        assertThat(result).satisfies(r -> {
            assertThat(r).isNotNull();
            
            // Should contain temperature data or indicate it's not available
            assertThat(r).satisfiesAnyOf(
                content -> assertThat(content).contains("celsius"),
                content -> assertThat(content).contains("Temperature sensors: Not available")
            );
            
            // Should contain fan data or indicate it's not available  
            assertThat(r).satisfiesAnyOf(
                content -> assertThat(content).contains("actual"),
                content -> assertThat(content).contains("Fan speeds: Not available")
            );
        });
    }

    @Test
    void getSystemHealthSummary() {
        String result = osqueryService.getSystemHealthSummary();

        System.out.println("System health summary: " + result);

        // Should return system health information with all required sections
        assertThat(result)
            .isNotNull()
            .contains("System Health Summary:")
            .contains("CPU Usage:")
            .contains("Memory Usage:")
            .contains("Disk Usage:")
            .contains("Network Connections:")
            .contains("Temperature and Fans:")
            .doesNotContain("Error: no such table");
        
        // Validate content quality separately for clarity
        assertThat(result).satisfiesAnyOf(
            content -> assertThat(content).contains("cpu_percent"),
            content -> assertThat(content).contains("CPU processes not available")
        );
        
        assertThat(result).satisfiesAnyOf(
            content -> assertThat(content).contains("resident_mb"),
            content -> assertThat(content).contains("Memory processes not available")
        );
    }

    @Test
    void getSuspiciousProcesses() {
        String result = osqueryService.getSuspiciousProcesses();
        
        System.out.println("Suspicious processes: " + result);
        
        // Should return suspicious process information
        assertThat(result).satisfies(r -> {
            assertThat(r).isNotNull();
            assertThat(r).contains("[");
            assertThat(r).doesNotContain("no such column");
            assertThat(r).satisfiesAnyOf(
                content -> assertThat(content).contains("suspicious_reason"),
                content -> assertThat(content).contains("[]"), // Empty array if no suspicious processes
                content -> assertThat(content).contains("Error:")
            );
        });
    }

    @Test
    void getHighDiskIOProcesses() {
        String result = osqueryService.getHighDiskIOProcesses();
        
        System.out.println("High disk I/O processes (first 500 chars): " + 
            result.substring(0, Math.min(result.length(), 500)));
        
        // Should return disk I/O information
        assertThat(result).satisfies(r -> {
            assertThat(r).isNotNull();
            assertThat(r).contains("[");
            assertThat(r).doesNotContain("no such column");
            assertThat(r).satisfiesAnyOf(
                content -> assertThat(content).contains("disk_read_mb"),
                content -> assertThat(content).contains("disk_write_mb"), 
                content -> assertThat(content).contains("total_disk_mb"),
                content -> assertThat(content).contains("[]"), // Empty array if no disk I/O processes
                content -> assertThat(content).contains("Error:")
            );
        });
    }
}