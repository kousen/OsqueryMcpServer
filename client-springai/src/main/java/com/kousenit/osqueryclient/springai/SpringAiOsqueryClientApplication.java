package com.kousenit.osqueryclient.springai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.*;

@SpringBootApplication
public class SpringAiOsqueryClientApplication {

    private final SyncMcpToolCallbackProvider toolCallbackProvider;
    private final ConfigurableApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean rawOutput = false;
    
    public SpringAiOsqueryClientApplication(SyncMcpToolCallbackProvider toolCallbackProvider, 
                                           ConfigurableApplicationContext context) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.context = context;
    }

    public static void main(String[] args) {
        System.setProperty("spring.shell.interactive.enabled", "false");
        SpringApplication.run(SpringAiOsqueryClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner() {
        return args -> {
            System.out.println("Spring AI MCP Client for Osquery");
            
            ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
            if (toolCallbacks == null || toolCallbacks.length == 0) {
                System.err.println("No MCP tools available. Make sure the server is configured properly.");
                return;
            }
            
            System.out.println("Found " + toolCallbacks.length + " MCP tools:");
            for (ToolCallback tool : toolCallbacks) {
                System.out.println("  - " + tool.getToolDefinition().name() + 
                                 ": " + tool.getToolDefinition().description());
            }
            
            if (args.length > 0) {
                String query = String.join(" ", args);
                if ("--raw".equals(args[0])) {
                    rawOutput = true;
                    if (args.length > 1) {
                        query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        processQuery(toolCallbacks, query);
                    } else {
                        runInteractiveMode(toolCallbacks);
                    }
                } else if ("--interactive".equals(args[0]) || "-i".equals(args[0])) {
                    runInteractiveMode(toolCallbacks);
                } else {
                    processQuery(toolCallbacks, query);
                }
            } else {
                runInteractiveMode(toolCallbacks);
            }
            
            // Shut down the Spring Boot application after completion
            System.exit(SpringApplication.exit(context));
        };
    }

    private void processQuery(ToolCallback[] toolCallbacks, String query) {
        try {
            String toolName = mapQueryToTool(query);
            
            ToolCallback tool = findTool(toolCallbacks, toolName);
            if (tool == null) {
                System.err.println("Tool not found: " + toolName);
                return;
            }
            
            System.out.println("Executing: " + toolName);
            
            String result;
            if (toolName.contains("executeOsquery")) {
                result = tool.call("{\"sql\":\"" + query.replace("\"", "\\\"") + "\"}");
            } else {
                result = tool.call("{}");
            }
            
            formatAndPrintResult(result, toolName);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runInteractiveMode(ToolCallback[] toolCallbacks) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Interactive Mode - Type 'help' for commands, 'exit' to quit");
        
        while (true) {
            System.out.print("osquery> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }
            
            if (input.equalsIgnoreCase("tools")) {
                listTools(toolCallbacks);
                continue;
            }
            
            if (input.equalsIgnoreCase("raw")) {
                rawOutput = !rawOutput;
                System.out.println("Output mode: " + (rawOutput ? "raw" : "formatted"));
                continue;
            }
            
            processQuery(toolCallbacks, input);
        }
        
        System.out.println("Goodbye!");
    }

    private void listTools(ToolCallback[] toolCallbacks) {
        System.out.println("\nAvailable tools:");
        for (ToolCallback tool : toolCallbacks) {
            System.out.println("  - " + tool.getToolDefinition().name() + 
                             ": " + tool.getToolDefinition().description());
        }
        System.out.println();
    }

    private ToolCallback findTool(ToolCallback[] toolCallbacks, String toolName) {
        for (ToolCallback tool : toolCallbacks) {
            if (tool.getToolDefinition().name().equals(toolName)) {
                return tool;
            }
        }
        return null;
    }

    private static String mapQueryToTool(String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        String prefix = "osquery_cli_osquery_server_";
        
        // Check for SQL queries first (highest priority)
        if (lowerQuery.startsWith("select ") || lowerQuery.contains(" from ")) {
            return prefix + "executeOsquery";
        }
        
        // Check for specific tool queries
        if (lowerQuery.contains("cpu")) {
            return prefix + "getHighCpuProcesses";
        } else if (lowerQuery.contains("memory") || lowerQuery.contains("ram")) {
            return prefix + "getHighMemoryProcesses";
        } else if (lowerQuery.contains("network") || lowerQuery.contains("connection") || lowerQuery.contains("connect")) {
            return prefix + "getNetworkConnections";
        } else if (lowerQuery.contains("temperature") || lowerQuery.contains("hot") || lowerQuery.contains("fan")) {
            return prefix + "getTemperatureInfo";
        } else if (lowerQuery.contains("health") || lowerQuery.contains("status")) {
            return prefix + "getSystemHealthSummary";
        } else if (lowerQuery.contains("tables")) {
            return prefix + "listOsqueryTables";
        } else {
            return prefix + "getSystemHealthSummary";
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable queries:");
        System.out.println("  - What's using my CPU?");
        System.out.println("  - What's using my memory?");
        System.out.println("  - Show network connections");
        System.out.println("  - Why is my fan running? / Check temperature");
        System.out.println("  - Show system health");
        System.out.println("  - List available tables");
        System.out.println("\nSQL queries (examples):");
        System.out.println("  - SELECT name, pid, cpu_time FROM processes ORDER BY cpu_time DESC LIMIT 5");
        System.out.println("  - SELECT * FROM system_info");
        System.out.println("\nCommands:");
        System.out.println("  help  - Show this help");
        System.out.println("  tools - List available MCP tools");
        System.out.println("  raw   - Toggle raw/formatted output (current: " + (rawOutput ? "raw" : "formatted") + ")");
        System.out.println("  exit  - Exit the program");
        System.out.println();
    }
    
    private void formatAndPrintResult(String result, String toolName) {
        if (rawOutput) {
            System.out.println(result);
            return;
        }
        
        try {
            // Parse the result as JSON array
            JsonNode rootNode = objectMapper.readTree(result);
            
            // Extract the text content from the first element
            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode firstElement = rootNode.get(0);
                if (firstElement.has("text")) {
                    String textContent = firstElement.get("text").asText();
                    
                    // The text content is a JSON string that needs to be unescaped
                    // It starts and ends with quotes that need to be removed
                    if (textContent.startsWith("\"") && textContent.endsWith("\"")) {
                        // Remove the outer quotes and unescape the JSON
                        textContent = textContent.substring(1, textContent.length() - 1);
                        textContent = textContent.replace("\\\"", "\"");
                        textContent = textContent.replace("\\\\", "\\");
                        textContent = textContent.replace("\\n", "\n");
                        textContent = textContent.replace("\\t", "\t");
                        textContent = textContent.replace("\\r", "\r");
                    }
                    
                    // Special handling for system health summary or temperature info
                    if (toolName.contains("getSystemHealthSummary") || toolName.contains("getTemperatureInfo")) {
                        printSystemHealthSummary(textContent);
                        return;
                    }
                    
                    // Try to parse the text content as JSON
                    try {
                        JsonNode dataNode = objectMapper.readTree(textContent);
                        if (dataNode.isArray() && dataNode.size() > 0) {
                            printAsTable(dataNode, toolName);
                        } else if (dataNode.isObject()) {
                            printAsKeyValue(dataNode);
                        } else {
                            System.out.println(textContent);
                        }
                    } catch (JsonProcessingException e) {
                        // If it's not JSON, just print the text
                        System.out.println(textContent);
                    }
                    return;
                }
            }
            
            // If we couldn't extract text, print the raw result
            System.out.println(result);
            
        } catch (JsonProcessingException e) {
            // If parsing fails, just print the raw result
            System.out.println(result);
        }
    }
    
    private JsonNode extractDataFromResponse(JsonNode rootNode) {
        try {
            // First, check if it's a simple string response
            if (rootNode.isTextual()) {
                String content = rootNode.asText();
                // Try to parse as JSON
                return objectMapper.readTree(content);
            }
            
            // Handle the response structure from Spring AI MCP
            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode firstElement = rootNode.get(0);
                if (firstElement.has("text")) {
                    String textContent = firstElement.get("text").asText();
                    // Parse the escaped JSON string
                    return objectMapper.readTree(textContent);
                }
            }
            
            return rootNode;
        } catch (Exception e) {
            // If all parsing fails, return null to indicate raw output should be used
            return null;
        }
    }
    
    private void printAsTable(JsonNode arrayNode, String toolName) {
        if (arrayNode.size() == 0) {
            System.out.println("No results found.");
            return;
        }
        
        // Get column names from the first object
        JsonNode firstObject = arrayNode.get(0);
        List<String> columns = new ArrayList<>();
        Iterator<String> fieldNames = firstObject.fieldNames();
        while (fieldNames.hasNext()) {
            columns.add(fieldNames.next());
        }
        
        // Calculate column widths
        Map<String, Integer> columnWidths = new HashMap<>();
        for (String column : columns) {
            int maxWidth = column.length();
            for (JsonNode row : arrayNode) {
                JsonNode value = row.get(column);
                if (value != null) {
                    int width = value.asText().length();
                    maxWidth = Math.max(maxWidth, width);
                }
            }
            columnWidths.put(column, Math.min(maxWidth + 2, 50)); // Cap at 50 chars
        }
        
        // Print header
        printTableSeparator(columns, columnWidths);
        System.out.print("|");
        for (String column : columns) {
            System.out.printf(" %-" + (columnWidths.get(column) - 2) + "s |", column);
        }
        System.out.println();
        printTableSeparator(columns, columnWidths);
        
        // Print rows
        for (JsonNode row : arrayNode) {
            System.out.print("|");
            for (String column : columns) {
                JsonNode value = row.get(column);
                String text = value != null ? value.asText() : "";
                if (text.length() > columnWidths.get(column) - 2) {
                    text = text.substring(0, columnWidths.get(column) - 5) + "...";
                }
                System.out.printf(" %-" + (columnWidths.get(column) - 2) + "s |", text);
            }
            System.out.println();
        }
        printTableSeparator(columns, columnWidths);
        
        System.out.println("\nTotal: " + arrayNode.size() + " rows");
    }
    
    private void printTableSeparator(List<String> columns, Map<String, Integer> columnWidths) {
        System.out.print("+");
        for (String column : columns) {
            System.out.print("-".repeat(columnWidths.get(column)));
            System.out.print("+");
        }
        System.out.println();
    }
    
    private void printAsKeyValue(JsonNode objectNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        int maxKeyLength = 0;
        
        // Find max key length for alignment
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            maxKeyLength = Math.max(maxKeyLength, field.getKey().length());
        }
        
        // Print key-value pairs
        fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            System.out.printf("%-" + (maxKeyLength + 2) + "s: %s\n", 
                field.getKey(), field.getValue().asText());
        }
    }
    
    private void printSystemHealthSummary(String summaryText) {
        // The system health summary contains multiple sections with JSON arrays
        // Parse and format each section separately
        String[] lines = summaryText.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.endsWith(":") && !line.startsWith("[")) {
                // This is a section header
                System.out.println("\n" + line);
            } else if (line.startsWith("[")) {
                // This is JSON data - parse and format it
                try {
                    JsonNode sectionData = objectMapper.readTree(line);
                    if (sectionData.isArray() && sectionData.size() > 0) {
                        printAsTable(sectionData, "");
                    }
                } catch (Exception e) {
                    // If parsing fails, just print the line
                    System.out.println(line);
                }
            } else if (!line.isEmpty()) {
                System.out.println(line);
            }
        }
    }
}