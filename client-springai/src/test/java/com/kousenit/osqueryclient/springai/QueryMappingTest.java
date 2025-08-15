package com.kousenit.osqueryclient.springai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Query Mapping Tests")
class QueryMappingTest {

    @ParameterizedTest
    @DisplayName("Should map CPU-related queries correctly")
    @ValueSource(strings = {
        "What's using my CPU?",
        "show cpu usage",
        "CPU intensive processes",
        "high cpu",
        "cpu load"
    })
    void shouldMapCpuQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getHighCpuProcesses");
    }

    @ParameterizedTest
    @DisplayName("Should map memory-related queries correctly")
    @ValueSource(strings = {
        "What's using my memory?",
        "show ram usage",
        "memory intensive processes", 
        "high memory",
        "ram consumption"
    })
    void shouldMapMemoryQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getHighMemoryProcesses");
    }

    @ParameterizedTest
    @DisplayName("Should map network-related queries correctly")
    @ValueSource(strings = {
        "show network connections",
        "network activity",
        "active connections"
    })
    void shouldMapNetworkQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getNetworkConnections");
    }

    @ParameterizedTest
    @DisplayName("Should map temperature-related queries correctly")
    @ValueSource(strings = {
        "why is my fan running?",
        "check temperature",
        "system is hot",
        "fan speed"
    })
    void shouldMapTemperatureQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getTemperatureInfo");
    }

    @ParameterizedTest
    @DisplayName("Should map health/status queries correctly")
    @ValueSource(strings = {
        "system health",
        "overall status",
        "health check",
        "system status"
    })
    void shouldMapHealthQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getSystemHealthSummary");
    }

    @ParameterizedTest
    @DisplayName("Should map table listing queries correctly")
    @ValueSource(strings = {
        "list tables",
        "show tables",
        "available tables"
    })
    void shouldMapTableQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_listOsqueryTables");
    }

    @ParameterizedTest
    @DisplayName("Should detect SQL queries correctly")
    @CsvSource({
        "'SELECT * FROM processes', osquery_cli_osquery_server_executeOsquery",
        "'select name from system_info', osquery_cli_osquery_server_executeOsquery", 
        "'SELECT pid, name FROM processes WHERE cpu_time > 1000', osquery_cli_osquery_server_executeOsquery",
        "'select * from users', osquery_cli_osquery_server_executeOsquery"
    })
    void shouldDetectSqlQueries(String query, String expectedTool) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo(expectedTool);
    }

    @Test
    @DisplayName("Should default to system health for unknown queries")
    void shouldDefaultToSystemHealth() throws Exception {
        String result = invokeMapQueryToTool("some random unknown query");
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getSystemHealthSummary");
    }

    @Test
    @DisplayName("Should handle empty and null queries")
    void shouldHandleEmptyQueries() throws Exception {
        assertThat(invokeMapQueryToTool("")).isEqualTo("osquery_cli_osquery_server_getSystemHealthSummary");
        assertThat(invokeMapQueryToTool("   ")).isEqualTo("osquery_cli_osquery_server_getSystemHealthSummary");
    }

    @Test
    @DisplayName("Should be case insensitive")
    void shouldBeCaseInsensitive() throws Exception {
        assertThat(invokeMapQueryToTool("CPU")).isEqualTo("osquery_cli_osquery_server_getHighCpuProcesses");
        assertThat(invokeMapQueryToTool("cpu")).isEqualTo("osquery_cli_osquery_server_getHighCpuProcesses");
        assertThat(invokeMapQueryToTool("CpU")).isEqualTo("osquery_cli_osquery_server_getHighCpuProcesses");
        
        assertThat(invokeMapQueryToTool("MEMORY")).isEqualTo("osquery_cli_osquery_server_getHighMemoryProcesses");
        assertThat(invokeMapQueryToTool("Memory")).isEqualTo("osquery_cli_osquery_server_getHighMemoryProcesses");
        
        assertThat(invokeMapQueryToTool("SELECT * FROM processes")).isEqualTo("osquery_cli_osquery_server_executeOsquery");
        assertThat(invokeMapQueryToTool("select * from processes")).isEqualTo("osquery_cli_osquery_server_executeOsquery");
    }

    @Test
    @DisplayName("Should handle edge cases correctly")
    void shouldHandleEdgeCases() throws Exception {
        // "what's connected" contains "connection" substring
        assertThat(invokeMapQueryToTool("what's connected to the internet")).isEqualTo("osquery_cli_osquery_server_getNetworkConnections");
        
        // SQL queries with different structures
        assertThat(invokeMapQueryToTool("SELECT pid, name FROM processes WHERE cpu_time > 1000")).isEqualTo("osquery_cli_osquery_server_executeOsquery");
        
        // Just "SELECT" without proper SQL structure should still be detected
        assertThat(invokeMapQueryToTool("SELECT")).isEqualTo("osquery_cli_osquery_server_getSystemHealthSummary");
    }

    @ParameterizedTest
    @DisplayName("Should handle complex queries with multiple keywords")
    @CsvSource({
        "'Show me processes using high CPU and memory', osquery_cli_osquery_server_getHighCpuProcesses", // CPU comes first
        "'Check network connections and system usage', osquery_cli_osquery_server_getNetworkConnections", // Network comes first  
        "'System health and overall status', osquery_cli_osquery_server_getSystemHealthSummary", // Health comes first
        "'SELECT cpu_time FROM processes WHERE memory > 1000', osquery_cli_osquery_server_executeOsquery" // SQL detected first
    })
    void shouldHandleComplexQueries(String query, String expectedTool) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo(expectedTool);
    }

    @ParameterizedTest
    @DisplayName("Should map security/suspicious process queries correctly")
    @ValueSource(strings = {
        "suspicious processes",
        "is my system compromised",
        "what looks suspicious",
        "security check",
        "malware detection",
        "unusual processes"
    })
    void shouldMapSecurityQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getSuspiciousProcesses");
    }

    @ParameterizedTest
    @DisplayName("Should map disk I/O related queries correctly")
    @ValueSource(strings = {
        "disk activity",
        "high disk io",
        "why is my disk busy",
        "disk slowdown",
        "what's writing to disk",
        "disk usage processes"
    })
    void shouldMapDiskIOQueries(String query) throws Exception {
        String result = invokeMapQueryToTool(query);
        assertThat(result).isEqualTo("osquery_cli_osquery_server_getHighDiskIOProcesses");
    }

    @Test
    @DisplayName("Should handle queries with multiple new keywords correctly")
    void shouldHandleNewKeywordPriorities() throws Exception {
        // Security keywords should have high priority
        assertThat(invokeMapQueryToTool("check for suspicious disk activity"))
            .isEqualTo("osquery_cli_osquery_server_getSuspiciousProcesses");
        
        // Disk keywords should match when no security keywords present
        assertThat(invokeMapQueryToTool("show high disk usage and memory"))
            .isEqualTo("osquery_cli_osquery_server_getHighDiskIOProcesses");
    }

    /**
     * Uses reflection to call the private static mapQueryToTool method for testing
     */
    private String invokeMapQueryToTool(String query) throws Exception {
        Method method = SpringAiOsqueryClientApplication.class.getDeclaredMethod("mapQueryToTool", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, query);
    }
}