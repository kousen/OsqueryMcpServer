package com.kousenit.osquerymcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OsqueryMcpServerApplication {

    static void main(String[] args) {
        // Suppress logback status messages before any logging initialization —
        // critical for STDIO MCP mode where stdout must be clean JSON-RPC only
        System.setProperty("logback.statusListenerClass",
                "ch.qos.logback.core.status.NopStatusListener");
        SpringApplication.run(OsqueryMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider osqueryTools(OsqueryService osqueryService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(osqueryService)
                .build();
    }
}
