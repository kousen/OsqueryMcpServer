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
    void testExecuteOsquery() {
        // Simple query to get osquery version
        String result = osqueryService.executeOsquery("SELECT version FROM osquery_info");
        
        System.out.println("Osquery version result: " + result);
        
        // Should return JSON format
        assertThat(result).isNotNull();
        assertThat(result).contains("{");
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void testListOsqueryTables() {
        String result = osqueryService.listOsqueryTables();
        
        System.out.println("Available tables (first 500 chars): " + 
            result.substring(0, Math.min(result.length(), 500)));
        
        // Should return JSON with table names
        assertThat(result).isNotNull();
        assertThat(result).contains("name");
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void testInvalidQuery() {
        String result = osqueryService.executeOsquery("INVALID SQL QUERY");
        
        // Osquery returns empty JSON array (with newlines) for invalid queries
        assertThat(result).isNotNull();
        assertThat(result.trim()).isEqualTo("[\n\n]");
    }

    @Test
    void testGetTableSchema() {
        String result = osqueryService.getTableSchema("processes");
        
        System.out.println("Processes table schema: " + result);
        
        // Should return schema information
        assertThat(result).isNotNull();
        assertThat(result).contains("name");
        assertThat(result).contains("type");
    }

    @Test
    void testGetHighCpuProcesses() {
        String result = osqueryService.getHighCpuProcesses();
        
        System.out.println("High CPU processes: " + result);
        
        // Should return process information
        assertThat(result).isNotNull();
        assertThat(result).contains("[");
    }
}