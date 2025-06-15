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
        assertThat(result).isNotNull();
        assertThat(result).contains("{");
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void listOsqueryTables() {
        String result = osqueryService.listOsqueryTables();
        
        System.out.println("Available tables (first 500 chars): " + 
            result.substring(0, Math.min(result.length(), 500)));
        
        // Should return JSON with table names
        assertThat(result).isNotNull();
        assertThat(result).contains("name");
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void invalidQuery() {
        String result = osqueryService.executeOsquery("INVALID SQL QUERY");
        
        // With ProcessBuilder, we now properly capture error messages
        assertThat(result).isNotNull();
        assertThat(result).startsWith("Error:");
        assertThat(result).contains("syntax error");
    }

    @Test
    void getTableSchema() {
        String result = osqueryService.getTableSchema("processes");
        
        System.out.println("Processes table schema: " + result);
        
        // Should return schema information
        assertThat(result).isNotNull();
        assertThat(result).contains("name");
        assertThat(result).contains("type");
    }

    @Test
    void getHighCpuProcesses() {
        String result = osqueryService.getHighCpuProcesses();
        
        System.out.println("High CPU processes: " + result);
        
        // Should return process information
        assertThat(result).isNotNull();
        assertThat(result).contains("[");
    }
    
    @Test
    void getCommonQueries() {
        String result = osqueryService.getCommonQueries();
        
        System.out.println("Common queries (first 200 chars): " + 
            result.substring(0, Math.min(result.length(), 200)));
        
        // Should contain example queries
        assertThat(result).isNotNull();
        assertThat(result).contains("Common diagnostic queries:");
        assertThat(result).contains("CPU consuming processes");
        assertThat(result).contains("Memory usage");
        assertThat(result).contains("Network connections");
    }
    
    @Test
    void getHighMemoryProcesses() {
        String result = osqueryService.getHighMemoryProcesses();
        
        System.out.println("High memory processes: " + result);
        
        // Should return process memory information
        assertThat(result).isNotNull();
        assertThat(result).contains("[");
        // Check for memory-specific fields
        assertThat(result).satisfiesAnyOf(
            r -> assertThat(r).contains("resident_mb"),
            r -> assertThat(r).contains("Error:")
        );
    }
    
    @Test
    void getNetworkConnections() {
        String result = osqueryService.getNetworkConnections();
        
        System.out.println("Network connections (first 500 chars): " + 
            result.substring(0, Math.min(result.length(), 500)));
        
        // Should return network connection information
        assertThat(result).isNotNull();
        assertThat(result).satisfiesAnyOf(
            r -> assertThat(r).contains("local_port"),
            r -> assertThat(r).contains("[]"), // Empty array if no connections
            r -> assertThat(r).contains("Error:")
        );
    }
    
    @Test
    void getTemperatureInfo() {
        String result = osqueryService.getTemperatureInfo();
        
        System.out.println("Temperature and fan info:\n" + result);
        
        // Should return temperature and fan information
        assertThat(result).isNotNull();
        
        // Should contain temperature data or indicate it's not available
        assertThat(result).satisfiesAnyOf(
            r -> assertThat(r).contains("celsius"),
            r -> assertThat(r).contains("Temperature sensors: Not available")
        );
        
        // Should contain fan data or indicate it's not available  
        assertThat(result).satisfiesAnyOf(
            r -> assertThat(r).contains("actual"),
            r -> assertThat(r).contains("Fan speeds: Not available")
        );
    }

    @Test
    void getSystemHealthSummary() {
        String result = osqueryService.getSystemHealthSummary();

        System.out.println("System health summary: " + result);

        // Should return system health information
        assertThat(result).isNotNull();
        assertThat(result).contains("System Health Summary:");
        assertThat(result).contains("CPU Usage:");
        assertThat(result).contains("Memory Usage:");
        assertThat(result).contains("Disk Usage:");
        assertThat(result).contains("Network Connections:");
        assertThat(result).contains("Temperature and Fans:");
        
        // Should not contain raw error messages
        assertThat(result).doesNotContain("Error: no such table");
        
        // Should have actual data or "Not available" messages
        assertThat(result).satisfiesAnyOf(
            r -> assertThat(r).contains("cpu_percent"),
            r -> assertThat(r).contains("CPU processes not available")
        );
        
        assertThat(result).satisfiesAnyOf(
            r -> assertThat(r).contains("resident_mb"),
            r -> assertThat(r).contains("Memory processes not available")
        );
    }
}