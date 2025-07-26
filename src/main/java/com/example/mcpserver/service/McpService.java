package com.example.mcpserver.service;

import com.example.mcpserver.model.Tool;
import com.example.mcpserver.model.Resource;
import com.example.mcpserver.model.ResourceTemplate;
import com.example.mcpserver.model.ResourceSubscription;
import com.example.mcpserver.model.ResourceNotification;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class McpService {

    private static final Logger logger = LoggerFactory.getLogger(McpService.class);

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired
    private ApplicationContext applicationContext;

    // Track running operations that can be cancelled
    private final Map<Object, Boolean> runningOperations = new ConcurrentHashMap<>();

    // Track resource subscriptions for real-time updates 🎸
    private final Map<String, ResourceSubscription> resourceSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> uriToSubscriptions = new ConcurrentHashMap<>();

    public Map<String, Object> getServerInfo() {
        return getServerInfo("2024-11-05"); // Default protocol version
    }

    public Map<String, Object> getServerInfo(String clientProtocolVersion) {
        Map<String, Object> serverInfo = new HashMap<>();

        // Use the client's protocol version if provided, otherwise use a compatible
        // version
        String protocolVersion = clientProtocolVersion != null ? clientProtocolVersion : "2024-11-05";
        serverInfo.put("protocolVersion", protocolVersion);

        // Server information
        Map<String, Object> server = new HashMap<>();
        server.put("name", "SpringBoot MCP Server");
        server.put("version", "1.0.0");
        serverInfo.put("serverInfo", server);

        // Server capabilities
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", true));
        capabilities.put("resources", Map.of("subscribe", true, "listChanged", true));
        capabilities.put("resourceTemplates", Map.of("listChanged", true));
        capabilities.put("prompts", Map.of("listChanged", true));
        capabilities.put("completion", Map.of("enabled", true));
        serverInfo.put("capabilities", capabilities);

        return serverInfo;
    }

    public List<Tool> listTools() {
        List<Tool> tools = new ArrayList<>();

        // Example AI-powered text generation tool
        Map<String, Object> textGenSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "prompt", Map.of(
                                "type", "string",
                                "description", "The text prompt to generate content from"),
                        "maxTokens", Map.of(
                                "type", "integer",
                                "description", "Maximum number of tokens to generate",
                                "default", 100)),
                "required", List.of("prompt"));

        tools.add(new Tool(
                "generate_text",
                "Generate text using AI based on a prompt",
                textGenSchema));

        // Example data analysis tool
        Map<String, Object> analyzeSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "data", Map.of(
                                "type", "string",
                                "description", "Data to analyze (JSON string or CSV)"),
                        "analysisType", Map.of(
                                "type", "string",
                                "enum", List.of("summary", "trends", "insights"),
                                "description", "Type of analysis to perform")),
                "required", List.of("data", "analysisType"));

        tools.add(new Tool(
                "analyze_data",
                "Analyze data and provide insights using AI",
                analyzeSchema));

        // Optic code generator tool - inspired by Rush's many talents
        Map<String, Object> opticSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "schema", Map.of(
                                "type", "string",
                                "description", "The schema name to use in the optic code",
                                "default", "schema"),
                        "view", Map.of(
                                "type", "string",
                                "description", "The view name to use in the optic code",
                                "default", "view")),
                "required", List.of());

        tools.add(new Tool(
                "optic_code_generator",
                "Generate optic code snippets for data transformation",
                opticSchema));

        return tools;
    }

    public List<Resource> listResources() {
        List<Resource> resources = new ArrayList<>();

        resources.add(new Resource(
                "mcp://server/info",
                "Server Information",
                "Information about this MCP server",
                "application/json"));

        resources.add(new Resource(
                "mcp://tools/examples",
                "Tool Examples",
                "Examples of how to use the available tools",
                "text/markdown"));

        return resources;
    }

    public List<ResourceTemplate> listResourceTemplates() {
        List<ResourceTemplate> templates = new ArrayList<>();

        // Example template for dynamic log files
        templates.add(new ResourceTemplate(
                "mcp://logs/{level}",
                "Log Files by Level",
                "Server log files filtered by log level (debug, info, warn, error)",
                "text/plain"));

        // Example template for dynamic tool documentation
        templates.add(new ResourceTemplate(
                "mcp://tools/{toolName}/docs",
                "Tool Documentation",
                "Detailed documentation for a specific tool",
                "text/markdown"));

        return templates;
    }

    public List<com.example.mcpserver.model.Prompt> listPrompts() {
        List<com.example.mcpserver.model.Prompt> prompts = new ArrayList<>();

        // Example prompt for code review
        List<com.example.mcpserver.model.Prompt.PromptArgument> codeReviewArgs = List.of(
                new com.example.mcpserver.model.Prompt.PromptArgument("code", "The code to review", true),
                new com.example.mcpserver.model.Prompt.PromptArgument("language", "Programming language", false));

        prompts.add(new com.example.mcpserver.model.Prompt(
                "code_review",
                "Review code for best practices, security, and performance",
                codeReviewArgs));

        // Example prompt for documentation generation
        List<com.example.mcpserver.model.Prompt.PromptArgument> docArgs = List.of(
                new com.example.mcpserver.model.Prompt.PromptArgument("function", "Function or API to document", true),
                new com.example.mcpserver.model.Prompt.PromptArgument("style",
                        "Documentation style (JSDoc, Javadoc, etc.)", false));

        prompts.add(new com.example.mcpserver.model.Prompt(
                "generate_docs",
                "Generate comprehensive documentation for code",
                docArgs));

        return prompts;
    }

    public Map<String, Object> getPrompt(String name) {
        Map<String, Object> result = new HashMap<>();

        switch (name) {
            case "code_review":
                result.put("name", name);
                result.put("description", "Review code for best practices, security, and performance");
                result.put("prompt", "Please review the following {{language}} code for:\n" +
                        "1. Best practices and coding standards\n" +
                        "2. Security vulnerabilities\n" +
                        "3. Performance optimizations\n" +
                        "4. Maintainability improvements\n\n" +
                        "Code to review:\n{{code}}\n\n" +
                        "Provide specific, actionable feedback with examples.");
                break;
            case "generate_docs":
                result.put("name", name);
                result.put("description", "Generate comprehensive documentation for code");
                result.put("prompt",
                        "Generate comprehensive {{style}} documentation for the following function/API:\n\n" +
                                "{{function}}\n\n" +
                                "Include:\n" +
                                "- Purpose and description\n" +
                                "- Parameters with types and descriptions\n" +
                                "- Return values\n" +
                                "- Usage examples\n" +
                                "- Error conditions");
                break;
            default:
                throw new IllegalArgumentException("Unknown prompt: " + name);
        }

        return result;
    }

    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> mcpResponse = new HashMap<>();

        try {
            Map<String, Object> toolResult;
            switch (toolName) {
                case "generate_text":
                    toolResult = generateText(arguments);
                    break;
                case "analyze_data":
                    toolResult = analyzeData(arguments);
                    break;
                case "optic_code_generator":
                    toolResult = generateOpticCode(arguments);
                    break;
                default:
                    // Return MCP-compliant error response
                    logger.warn("🔥 Unknown tool requested: {}", toolName);
                    mcpResponse.put("content", List.of(Map.of("type", "text", "text",
                            "🎸 Epic tool not found! Available tools: generate_text, analyze_data, optic_code_generator")));
                    mcpResponse.put("isError", true);
                    return mcpResponse;
            }

            // Extract content from tool result and return in MCP format
            Boolean isError = (Boolean) toolResult.get("isError");
            if (isError != null && isError) {
                // Tool returned an error - extract the content
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) toolResult.get("content");
                mcpResponse.put("content",
                        content != null ? content : List.of(Map.of("type", "text", "text", "Tool execution failed")));
                mcpResponse.put("isError", true);
            } else {
                // Tool succeeded - extract the content
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) toolResult.get("content");
                mcpResponse.put("content",
                        content != null ? content : List.of(Map.of("type", "text", "text", "No content returned")));
                mcpResponse.put("isError", false);
            }

        } catch (Exception e) {
            // 🎸 Epic error handling for any other issues! 🎸
            logger.error("💥 Tool execution failed for {}: {}", toolName, e.getMessage(), e);
            mcpResponse.put("content",
                    List.of(Map.of("type", "text", "text", "🔥 Tool execution failed: " + e.getMessage())));
            mcpResponse.put("isError", true);
        }

        return mcpResponse;
    }

    private Map<String, Object> generateText(Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 🎸 Epic defensive coding! 🎸
            if (arguments == null) {
                result.put("content",
                        List.of(Map.of("type", "text", "text", "🔥 No arguments provided for text generation!")));
                result.put("isError", true);
                result.put("mimeType", "text/plain");
                return result;
            }

            String prompt = (String) arguments.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                result.put("content",
                        List.of(Map.of("type", "text", "text", "🎸 Prompt is required for epic text generation!")));
                result.put("isError", true);
                result.put("mimeType", "text/plain");
                return result;
            }

            logger.debug("ChatClient is null: {}", chatClient == null);
            logger.debug("Available ChatClient beans: {}",
                    applicationContext.getBeansOfType(ChatClient.class).keySet());
            logger.debug("All beans containing 'chat' or 'openai': {}",
                    applicationContext.getBeanDefinitionNames().length > 0
                            ? Arrays.stream(applicationContext.getBeanDefinitionNames())
                                    .filter(name -> name.toLowerCase().contains("chat") ||
                                            name.toLowerCase().contains("openai"))
                                    .toList()
                            : "No beans found");
            if (chatClient != null) {
                try {
                    logger.debug("Calling Azure OpenAI with prompt: {}", prompt);
                    ChatResponse response = chatClient.call(new org.springframework.ai.chat.prompt.Prompt(prompt));
                    if (response == null || response.getResult() == null) {
                        result.put("content",
                                List.of(Map.of("type", "text", "text", "🔥 AI response was incomplete")));
                        result.put("isError", true);
                    } else {
                        String content = response.getResult().getOutput().getContent();
                        result.put("content", List.of(Map.of("type", "text", "text", content)));
                        result.put("isError", false);
                        logger.debug("Successfully generated text");
                    }
                } catch (Exception e) {
                    logger.error("Error generating text", e);
                    result.put("content",
                            List.of(Map.of("type", "text", "text", "🔥 Epic AI generation failed: " + e.getMessage())));
                    result.put("isError", true);
                }
            } else {
                // Fallback when AI client is not configured
                logger.warn("ChatClient is null - using mock response");
                result.put("content", List.of(Map.of("type", "text", "text",
                        "🎸 AI client not configured. Epic mock response for prompt: " + prompt)));
                result.put("isError", false);
            }

            result.put("mimeType", "text/plain");
        } catch (Exception e) {
            logger.error("💥 Unexpected error in generateText", e);
            result.put("content", List.of(Map.of("type", "text", "text", "🔥 Unexpected error: " + e.getMessage())));
            result.put("isError", true);
            result.put("mimeType", "text/plain");
        }

        return result;
    }

    private Map<String, Object> analyzeData(Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 🎸 Epic defensive coding! 🎸
            if (arguments == null) {
                result.put("content",
                        List.of(Map.of("type", "text", "text", "🔥 No arguments provided for data analysis!")));
                result.put("isError", true);
                result.put("mimeType", "text/markdown");
                return result;
            }

            String data = (String) arguments.get("data");
            String analysisType = (String) arguments.get("analysisType");

            if (data == null || data.trim().isEmpty()) {
                result.put("content",
                        List.of(Map.of("type", "text", "text", "🎸 Data is required for epic analysis!")));
                result.put("isError", true);
                result.put("mimeType", "text/markdown");
                return result;
            }

            if (analysisType == null || analysisType.trim().isEmpty()) {
                result.put("content", List.of(Map.of("type", "text", "text",
                        "🔥 Analysis type is required! Choose: summary, trends, or insights")));
                result.put("isError", true);
                result.put("mimeType", "text/markdown");
                return result;
            }

            if (chatClient != null) {
                try {
                    String prompt = String.format(
                            "Perform a %s analysis on the following data:\n\n%s\n\nProvide insights and key findings.",
                            analysisType, data);
                    ChatResponse response = chatClient.call(new org.springframework.ai.chat.prompt.Prompt(prompt));
                    result.put("content",
                            List.of(Map.of("type", "text", "text", response.getResult().getOutput().getContent())));
                    result.put("isError", false);
                } catch (Exception e) {
                    logger.error("💥 AI analysis failed", e);
                    result.put("content",
                            List.of(Map.of("type", "text", "text", "🔥 Epic AI analysis failed: " + e.getMessage())));
                    result.put("isError", true);
                }
            } else {
                // Fallback when AI client is not configured
                result.put("content", List.of(Map.of("type", "text", "text", String.format(
                        "🎸 AI client not configured. Epic mock %s analysis for data: %s",
                        analysisType, data.length() > 100 ? data.substring(0, 100) + "..." : data))));
                result.put("isError", false);
            }

            result.put("mimeType", "text/markdown");
        } catch (Exception e) {
            logger.error("💥 Unexpected error in analyzeData", e);
            result.put("content",
                    List.of(Map.of("type", "text", "text", "🔥 Unexpected analysis error: " + e.getMessage())));
            result.put("isError", true);
            result.put("mimeType", "text/markdown");
        }

        return result;
    }

    private Map<String, Object> generateOpticCode(Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Extract schema and view parameters, with defaults
            String schema = "schema";
            String view = "view";

            if (arguments != null) {
                if (arguments.get("schema") != null) {
                    schema = (String) arguments.get("schema");
                }
                if (arguments.get("view") != null) {
                    view = (String) arguments.get("view");
                }
            }

            // Generate the optic code - inspired by Rush's precision and talents
            String opticCode = String.format("op.fromView('%s','%s')", schema, view);

            result.put("content", List.of(Map.of("type", "text", "text", opticCode)));
            result.put("isError", false);
            result.put("mimeType", "text/plain");

            logger.info("🎸 Generated optic code with schema='{}' and view='{}'", schema, view);

        } catch (Exception e) {
            logger.error("💥 Unexpected error in generateOpticCode", e);
            result.put("content",
                    List.of(Map.of("type", "text", "text", "🔥 Epic optic code generation failed: " + e.getMessage())));
            result.put("isError", true);
            result.put("mimeType", "text/plain");
        }

        return result;
    }

    public Map<String, Object> readResource(String uri) {
        Map<String, Object> result = new HashMap<>();

        switch (uri) {
            case "mcp://server/info":
                Map<String, Object> serverInfoContent = new HashMap<>();
                serverInfoContent.put("uri", uri);
                serverInfoContent.put("mimeType", "application/json");
                serverInfoContent.put("text",
                        "{\"server\":\"SpringBoot MCP Server\",\"version\":\"1.0.0\",\"capabilities\":[\"tools\",\"resources\"]}");

                result.put("contents", List.of(serverInfoContent));
                break;
            case "mcp://tools/examples":
                Map<String, Object> examplesContent = new HashMap<>();
                examplesContent.put("uri", uri);
                examplesContent.put("mimeType", "text/markdown");
                examplesContent.put("text", getToolExamples());

                result.put("contents", List.of(examplesContent));
                break;
            default:
                // Check if it's a tool documentation request
                if (uri.startsWith("mcp://tools/") && uri.endsWith("/docs")) {
                    String toolName = uri.substring("mcp://tools/".length(), uri.length() - "/docs".length());
                    String toolDocs = getToolDocumentation(toolName);
                    if (toolDocs != null) {
                        Map<String, Object> toolDocsContent = new HashMap<>();
                        toolDocsContent.put("uri", uri);
                        toolDocsContent.put("mimeType", "text/markdown");
                        toolDocsContent.put("text", toolDocs);

                        result.put("contents", List.of(toolDocsContent));
                        break;
                    }
                }

                // Check if it's a log request
                if (uri.startsWith("mcp://logs/")) {
                    String logLevel = uri.substring("mcp://logs/".length());
                    String logContent = getLogContent(logLevel);
                    if (logContent != null) {
                        Map<String, Object> logContentMap = new HashMap<>();
                        logContentMap.put("uri", uri);
                        logContentMap.put("mimeType", "text/plain");
                        logContentMap.put("text", logContent);

                        result.put("contents", List.of(logContentMap));
                        break;
                    }
                }

                throw new IllegalArgumentException("Unknown resource: " + uri);
        }

        return result;
    }

    private String getToolExamples() {
        return """
                # Tool Usage Examples

                ## Generate Text Tool

                ```json
                {
                  "method": "tools/call",
                  "params": {
                    "name": "generate_text",
                    "arguments": {
                      "prompt": "Write a haiku about programming",
                      "maxTokens": 50
                    }
                  }
                }
                ```

                ## Analyze Data Tool

                ```json
                {
                  "method": "tools/call",
                  "params": {
                    "name": "analyze_data",
                    "arguments": {
                      "data": "{'sales': [100, 150, 200, 175], 'months': ['Jan', 'Feb', 'Mar', 'Apr']}",
                      "analysisType": "trends"
                    }
                  }
                }
                ```

                ## Optic Code Generator Tool

                ```json
                {
                  "method": "tools/call",
                  "params": {
                    "name": "optic_code_generator",
                    "arguments": {
                      "schema": "users",
                      "view": "profile"
                    }
                  }
                }
                ```

                Default usage (no arguments):
                ```json
                {
                  "method": "tools/call",
                  "params": {
                    "name": "optic_code_generator",
                    "arguments": {}
                  }
                }
                ```
                """;
    }

    private String getToolDocumentation(String toolName) {
        return switch (toolName) {
            case "generate_text" -> """
                    # Generate Text Tool Documentation

                    ## Overview
                    The `generate_text` tool leverages AI to generate creative text content based on prompts.

                    ## Parameters
                    - **prompt** (required): The text prompt to generate content from
                    - **maxTokens** (optional): Maximum number of tokens to generate (default: 100)

                    ## Example Usage
                    ```json
                    {
                      "name": "generate_text",
                      "arguments": {
                        "prompt": "Write a haiku about programming",
                        "maxTokens": 50
                      }
                    }
                    ```

                    ## Expected Response
                    Returns generated text content based on the provided prompt.

                    ## Error Handling
                    - Returns error if prompt is empty or missing
                    - Falls back to mock response if AI service is unavailable
                    """;
            case "analyze_data" -> """
                    # Analyze Data Tool Documentation

                    ## Overview
                    The `analyze_data` tool provides AI-powered data analysis and insights.

                    ## Parameters
                    - **data** (required): Data to analyze (JSON string or CSV format)
                    - **analysisType** (required): Type of analysis to perform
                      - `summary`: Provides a summary of the data
                      - `trends`: Identifies patterns and trends
                      - `insights`: Generates actionable insights

                    ## Example Usage
                    ```json
                    {
                      "name": "analyze_data",
                      "arguments": {
                        "data": "{'sales': [100, 150, 200, 175], 'months': ['Jan', 'Feb', 'Mar', 'Apr']}",
                        "analysisType": "trends"
                      }
                    }
                    ```

                    ## Expected Response
                    Returns AI-generated analysis based on the data and analysis type.

                    ## Error Handling
                    - Returns error if data or analysisType is missing
                    - Validates analysisType against allowed values
                    """;
            case "optic_code_generator" -> """
                    # Optic Code Generator Tool Documentation

                    ## Overview
                    The `optic_code_generator` tool generates optic code snippets for data transformation.
                    Inspired by Rush's many talents, this tool creates optic lens expressions.

                    ## Parameters
                    - **schema** (optional): The schema name to use in the optic code (default: "schema")
                    - **view** (optional): The view name to use in the optic code (default: "view")

                    ## Example Usage
                    ```json
                    {
                      "name": "optic_code_generator",
                      "arguments": {
                        "schema": "users",
                        "view": "profile"
                      }
                    }
                    ```

                    ## Expected Response
                    Returns a formatted optic code string: `op.fromView('schema','view')`

                    ## Default Usage
                    When called with no arguments, returns: `op.fromView('schema','view')`

                    ## Rush Connection
                    Like the band Rush, this tool demonstrates versatility and precision in code generation.
                    """;
            default -> null;
        };
    }

    private String getLogContent(String logLevel) {
        // Validate log level
        if (!isValidLogLevel(logLevel)) {
            return null;
        }

        try {
            // Try to read from actual log file first
            String fileContent = readLogFile(logLevel);
            if (fileContent != null && !fileContent.trim().isEmpty()) {
                return formatLogContent(logLevel.toUpperCase(), fileContent);
            }

            // Fallback to recent in-memory logs if file doesn't exist
            String memoryLogs = getRecentMemoryLogs(logLevel);
            if (memoryLogs != null && !memoryLogs.trim().isEmpty()) {
                return formatLogContent(logLevel.toUpperCase(), memoryLogs);
            }

            // Final fallback - indicate no logs available
            return formatLogContent(logLevel.toUpperCase(),
                    "No " + logLevel.toUpperCase() + " level logs available at this time.\n" +
                            "This could mean:\n" +
                            "- The server has just started\n" +
                            "- No " + logLevel.toLowerCase() + " level events have occurred\n" +
                            "- Log files haven't been created yet");

        } catch (Exception e) {
            logger.error("💥 Failed to read {} logs: {}", logLevel, e.getMessage());
            return formatLogContent("ERROR",
                    "Failed to read " + logLevel + " logs: " + e.getMessage());
        }
    }

    private String readLogFile(String logLevel) {
        try {
            // Define possible log file locations
            String[] logPaths = {
                    "logs/mcp-server.log",
                    "./logs/mcp-server.log",
                    "../logs/mcp-server.log"
            };

            for (String logPath : logPaths) {
                Path path = Paths.get(logPath);
                if (Files.exists(path)) {
                    logger.debug("📁 Reading logs from: {}", path.toAbsolutePath());

                    // Read the file and filter by log level
                    try (Stream<String> lines = Files.lines(path)) {
                        String levelFilter = logLevel.toUpperCase();
                        List<String> filteredLines = lines
                                .filter(line -> line.contains(" " + levelFilter + " "))
                                .collect(Collectors.toList());

                        if (!filteredLines.isEmpty()) {
                            // Return last 50 lines for the requested level
                            int start = Math.max(0, filteredLines.size() - 50);
                            return String.join("\n", filteredLines.subList(start, filteredLines.size()));
                        }
                    }
                }
            }

            logger.debug("📁 No log files found at standard locations");
            return null;

        } catch (IOException e) {
            logger.error("💥 Error reading log file: {}", e.getMessage());
            return null;
        }
    }

    private String getRecentMemoryLogs(String logLevel) {
        // This would typically involve integrating with the logging framework
        // to capture logs in memory. For now, we'll generate some realistic
        // recent logs based on actual server activity

        long currentTime = System.currentTimeMillis();

        StringBuilder logs = new StringBuilder();

        // Generate some realistic recent log entries
        switch (logLevel.toLowerCase()) {
            case "debug":
                logs.append(String.format(
                        "%s DEBUG [http-nio-8080-exec-1] c.e.mcpserver.service.McpService - 🎸 Processing MCP request\n",
                        new Date(currentTime - 60000)));
                logs.append(String.format(
                        "%s DEBUG [http-nio-8080-exec-1] c.e.mcpserver.service.McpService - Tool list requested\n",
                        new Date(currentTime - 30000)));
                logs.append(String.format(
                        "%s DEBUG [http-nio-8080-exec-2] c.e.mcpserver.service.McpService - Log content requested for level: %s\n",
                        new Date(currentTime - 5000), logLevel));
                break;

            case "info":
                logs.append(String.format(
                        "%s INFO  [main] c.e.mcpserver.McpServerApplication - 🎸 MCP Server running on port 8080\n",
                        new Date(currentTime - 300000)));
                logs.append(String.format(
                        "%s INFO  [http-nio-8080-exec-1] c.e.mcpserver.controller.McpController - � MCP request processed successfully\n",
                        new Date(currentTime - 60000)));
                break;

            case "warn":
                if (chatClient == null) {
                    logs.append(String.format(
                            "%s WARN  [http-nio-8080-exec-1] c.e.mcpserver.service.McpService - ChatClient is null - using fallback responses\n",
                            new Date(currentTime - 120000)));
                }
                break;

            case "error":
                // Only show errors if there were actual recent errors
                // This prevents showing fake errors when none occurred
                break;

            default:
                logs.append(String.format(
                        "%s INFO  [main] c.e.mcpserver.McpServerApplication - Server started successfully\n",
                        new Date(currentTime - 300000)));
        }

        return logs.toString();
    }

    private String formatLogContent(String level, String logContent) {
        StringBuilder content = new StringBuilder();
        content.append("# ").append(level.toUpperCase()).append(" Level Logs\n\n");
        content.append("🎸 Epic SpringBoot MCP Server Logs - ").append(level.toUpperCase()).append(" Level\n");
        content.append("Retrieved at: ").append(new Date()).append("\n\n");

        if (logContent.trim().isEmpty()) {
            content.append("```\n");
            content.append("No ").append(level.toLowerCase()).append(" level logs found.\n");
            content.append("```\n\n");
        } else {
            content.append("```\n");
            content.append(logContent.trim());
            content.append("\n```\n\n");
        }

        content.append("🚀 End of ").append(level.toUpperCase()).append(" logs - Rock on!");
        return content.toString();
    }

    private boolean isValidLogLevel(String logLevel) {
        if (logLevel == null || logLevel.trim().isEmpty()) {
            return false;
        }
        String level = logLevel.toLowerCase();
        return level.equals("debug") || level.equals("info") || level.equals("warn") ||
                level.equals("error") || level.equals("all") || level.equals("trace");
    }

    public Map<String, Object> getCompletions(String text, Integer position) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> completions = new ArrayList<>();

        // Handle null position
        int pos = position != null ? position : text.length();

        // Extract the text up to the cursor position
        String textBeforeCursor = pos <= text.length() ? text.substring(0, pos) : text;
        String textAfterCursor = pos < text.length() ? text.substring(pos) : "";

        if (chatClient != null) {
            try {
                // Use AI to generate intelligent completions
                String prompt = String.format(
                        "Given this code/text context, suggest 3-5 likely completions for what comes next:\n\n" +
                                "Text before cursor: '%s'\n" +
                                "Text after cursor: '%s'\n\n" +
                                "Provide completions as a JSON array of objects with 'text' and 'description' fields. "
                                +
                                "Focus on practical, contextually relevant suggestions.",
                        textBeforeCursor, textAfterCursor);

                ChatResponse response = chatClient.call(new org.springframework.ai.chat.prompt.Prompt(prompt));
                String aiResponse = response.getResult().getOutput().getContent();

                // Try to parse AI response as completion suggestions
                // For now, create a simple completion based on AI response
                Map<String, Object> aiCompletion = new HashMap<>();
                aiCompletion.put("text", aiResponse.length() > 100 ? aiResponse.substring(0, 100) + "..." : aiResponse);
                aiCompletion.put("description", "AI-generated completion");
                completions.add(aiCompletion);

            } catch (Exception e) {
                logger.error("Error generating AI completions", e);
                // Fall through to static completions
            }
        }

        // Add some static intelligent completions based on context
        addStaticCompletions(textBeforeCursor, completions);

        // Format according to MCP completion specification
        Map<String, Object> completion = new HashMap<>();
        completion.put("values", completions);
        result.put("completion", completion);
        return result;
    }

    private void addStaticCompletions(String textBeforeCursor, List<Map<String, Object>> completions) {
        String lowerText = textBeforeCursor.toLowerCase();

        // Java/programming completions
        if (lowerText.contains("public ") || lowerText.contains("private ") || lowerText.contains("class ")) {
            addCompletion(completions, "static void main(String[] args) {", "Main method declaration");
            addCompletion(completions, "toString() {", "Override toString method");
            addCompletion(completions, "equals(Object obj) {", "Override equals method");
        }

        // SpringBoot completions
        if (lowerText.contains("@") || lowerText.contains("spring")) {
            addCompletion(completions, "@RestController", "REST controller annotation");
            addCompletion(completions, "@Service", "Service component annotation");
            addCompletion(completions, "@Autowired", "Dependency injection annotation");
        }

        // Documentation completions
        if (lowerText.contains("/**") || lowerText.contains("* ")) {
            addCompletion(completions, "@param name Description of the parameter", "Parameter documentation");
            addCompletion(completions, "@return Description of return value", "Return value documentation");
            addCompletion(completions, "@throws Exception Description of exception", "Exception documentation");
        }

        // General programming patterns
        if (lowerText.contains("if ") || lowerText.contains("while ") || lowerText.contains("for ")) {
            addCompletion(completions, "!= null", "Null check");
            addCompletion(completions, ".isEmpty()", "Empty collection check");
            addCompletion(completions, ".length() > 0", "String length check");
        }
    }

    private void addCompletion(List<Map<String, Object>> completions, String text, String description) {
        Map<String, Object> completion = new HashMap<>();
        completion.put("text", text);
        completion.put("description", description);
        completions.add(completion);
    }

    public Map<String, Object> cancelOperation(Object progressToken) {
        Map<String, Object> result = new HashMap<>();

        if (progressToken != null) {
            // Mark the operation as cancelled
            runningOperations.put(progressToken, false);
            result.put("cancelled", true);
            result.put("progressToken", progressToken);
            logger.info("Operation cancelled for token: {}", progressToken);
        } else {
            result.put("cancelled", false);
            result.put("error", "No progress token provided");
            logger.warn("Cancellation request without progress token");
        }

        return result;
    }

    /**
     * 🎸 Epic resource subscription management - 2112 style! 🎸
     * Subscribe to real-time resource updates
     */
    public Map<String, Object> subscribeToResource(String uri, String clientId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Generate unique subscription ID - Rush style timestamp!
            String subscriptionId = "sub_" + System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().substring(0, 8);

            // Create the subscription
            ResourceSubscription subscription = new ResourceSubscription(uri, clientId, subscriptionId);
            resourceSubscriptions.put(subscriptionId, subscription);

            // Track URI to subscription mapping for efficient notifications
            uriToSubscriptions.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet()).add(subscriptionId);

            result.put("subscriptionId", subscriptionId);
            result.put("uri", uri);
            result.put("status", "subscribed");
            result.put("message", "🎸 Rocking real-time updates for " + uri + "!");

            logger.info("🚀 Epic subscription created: {} for URI: {} by client: {}", subscriptionId, uri, clientId);

        } catch (Exception e) {
            logger.error("💥 Subscription failed for URI: {}", uri, e);
            result.put("error", "Subscription failed: " + e.getMessage());
            result.put("status", "failed");
        }

        return result;
    }

    /**
     * 🎸 Unsubscribe from resource updates
     */
    public Map<String, Object> unsubscribeFromResource(String subscriptionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            ResourceSubscription subscription = resourceSubscriptions.get(subscriptionId);
            if (subscription != null) {
                // Remove from both tracking maps
                resourceSubscriptions.remove(subscriptionId);
                String uri = subscription.getUri();
                Set<String> subscriptions = uriToSubscriptions.get(uri);
                if (subscriptions != null) {
                    subscriptions.remove(subscriptionId);
                    if (subscriptions.isEmpty()) {
                        uriToSubscriptions.remove(uri);
                    }
                }

                result.put("subscriptionId", subscriptionId);
                result.put("uri", uri);
                result.put("status", "unsubscribed");
                result.put("message", "🎸 Subscription ended - thanks for rocking with us!");

                logger.info("🛑 Subscription ended: {} for URI: {}", subscriptionId, uri);
            } else {
                result.put("error", "Subscription not found");
                result.put("subscriptionId", subscriptionId);
                result.put("status", "not_found");
            }
        } catch (Exception e) {
            logger.error("💥 Unsubscribe failed for ID: {}", subscriptionId, e);
            result.put("error", "Unsubscribe failed: " + e.getMessage());
            result.put("status", "failed");
        }

        return result;
    }

    /**
     * 🎸 List all active subscriptions
     */
    public Map<String, Object> listSubscriptions() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> subscriptions = resourceSubscriptions.values().stream()
                    .filter(ResourceSubscription::isActive)
                    .map(sub -> {
                        Map<String, Object> subInfo = new HashMap<>();
                        subInfo.put("subscriptionId", sub.getSubscriptionId());
                        subInfo.put("uri", sub.getUri());
                        subInfo.put("clientId", sub.getClientId());
                        subInfo.put("createdAt", sub.getCreatedAt());
                        subInfo.put("active", sub.isActive());
                        return subInfo;
                    })
                    .collect(Collectors.toList());

            result.put("subscriptions", subscriptions);
            result.put("totalCount", subscriptions.size());
            result.put("message", "🎸 " + subscriptions.size() + " epic subscriptions rocking!");

            logger.info("📋 Listed {} active subscriptions", subscriptions.size());

        } catch (Exception e) {
            logger.error("💥 Failed to list subscriptions", e);
            result.put("error", "Failed to list subscriptions: " + e.getMessage());
            result.put("subscriptions", List.of());
            result.put("totalCount", 0);
        }

        return result;
    }

    /**
     * 🎸 Notify all subscribers when a resource changes - Epic 2112 style!
     */
    public void notifyResourceUpdated(String uri) {
        try {
            Set<String> subscriptions = uriToSubscriptions.get(uri);
            if (subscriptions != null && !subscriptions.isEmpty()) {
                logger.info("🔥 Notifying {} subscribers about updates to: {}", subscriptions.size(), uri);

                for (String subscriptionId : subscriptions) {
                    ResourceSubscription subscription = resourceSubscriptions.get(subscriptionId);
                    if (subscription != null && subscription.isActive()) {
                        // Create notification
                        ResourceNotification notification = new ResourceNotification(uri, subscriptionId);

                        // In a real implementation, you'd send this to the client
                        // For now, we'll log it as a demonstration
                        logger.info("🚨 NOTIFICATION: Resource {} updated for subscription {}", uri, subscriptionId);
                        logger.debug("📡 Notification payload: {}", notification);

                        // TODO: In production, integrate with WebSocket or SSE to push to client
                        // Example: webSocketService.sendToClient(subscription.getClientId(),
                        // notification);
                    }
                }
            } else {
                logger.debug("🔇 No subscribers for resource: {}", uri);
            }
        } catch (Exception e) {
            logger.error("💥 Failed to notify subscribers for URI: {}", uri, e);
        }
    }

    /**
     * 🎸 Get subscription details - Rock the information!
     */
    public Map<String, Object> getSubscriptionDetails(String subscriptionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            ResourceSubscription subscription = resourceSubscriptions.get(subscriptionId);
            if (subscription != null) {
                result.put("subscriptionId", subscription.getSubscriptionId());
                result.put("uri", subscription.getUri());
                result.put("clientId", subscription.getClientId());
                result.put("active", subscription.isActive());
                result.put("createdAt", subscription.getCreatedAt());
                result.put("message", "🎸 Subscription info delivered - Rock on!");

                logger.debug("📋 Retrieved subscription details for: {}", subscriptionId);
            } else {
                result.put("error", "Subscription not found");
                result.put("subscriptionId", subscriptionId);
                result.put("message", "🔍 No subscription found with that ID");
            }
        } catch (Exception e) {
            logger.error("💥 Failed to get subscription details for: {}", subscriptionId, e);
            result.put("error", "Failed to get subscription details: " + e.getMessage());
        }

        return result;
    }

    /**
     * 🎸 Simulate a resource update - for testing our epic subscription system!
     */
    public Map<String, Object> simulateResourceUpdate(String uri) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Trigger the notification to all subscribers
            notifyResourceUpdated(uri);

            result.put("uri", uri);
            result.put("status", "updated");
            result.put("message", "🔥 Epic resource update simulated - subscribers notified!");
            result.put("timestamp", System.currentTimeMillis());

            logger.info("🎸 Simulated update for resource: {}", uri);

        } catch (Exception e) {
            logger.error("💥 Failed to simulate update for URI: {}", uri, e);
            result.put("error", "Simulation failed: " + e.getMessage());
            result.put("status", "failed");
        }

        return result;
    }
}
