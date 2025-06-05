package com.kousenit.osquerymcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class OsqueryService {
    @Tool(description = """
         Execute osquery SQL queries to inspect system state.
         Query processes, users, network connections, and other OS data.
         Example: SELECT name, pid FROM processes""")
    public String executeOsquery(String sql) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"osqueryi", "--json", sql}
            );
            return new String(p.getInputStream().readAllBytes());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "List available osquery tables on this system")
    public String listOsqueryTables() {
        return executeOsquery(
                "SELECT name FROM osquery_registry WHERE active = 1");
    }
}
