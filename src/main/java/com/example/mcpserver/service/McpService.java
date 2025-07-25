package com.example.mcpserver.service;

import com.example.mcpserver.model.Tool;
import com.example.mcpserver.model.Resource;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class McpService {

    private static final Logger logger = LoggerFactory.getLogger(McpService.class);

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired
    private ApplicationContext applicationContext;

    public Map<String, Object> getServerInfo() {
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "SpringBoot MCP Server");
        serverInfo.put("version", "1.0.0");

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", true));
        capabilities.put("resources", Map.of("subscribe", true, "listChanged", true));
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

    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();

        switch (toolName) {
            case "generate_text":
                result = generateText(arguments);
                break;
            case "analyze_data":
                result = analyzeData(arguments);
                break;
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        return result;
    }

    private Map<String, Object> generateText(Map<String, Object> arguments) {
        String prompt = (String) arguments.get("prompt");

        Map<String, Object> result = new HashMap<>();

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
                ChatResponse response = chatClient.call(new Prompt(prompt));
                result.put("content", response.getResult().getOutput().getContent());
                result.put("isError", false);
                logger.debug("Successfully generated text");
            } catch (Exception e) {
                logger.error("Error generating text", e);
                result.put("content", "Error generating text: " + e.getMessage());
                result.put("isError", true);
            }
        } else {
            // Fallback when AI client is not configured
            logger.warn("ChatClient is null - using mock response");
            result.put("content", "AI client not configured. This is a mock response for prompt: " + prompt);
            result.put("isError", false);
        }

        result.put("mimeType", "text/plain");
        return result;
    }

    private Map<String, Object> analyzeData(Map<String, Object> arguments) {
        String data = (String) arguments.get("data");
        String analysisType = (String) arguments.get("analysisType");

        Map<String, Object> result = new HashMap<>();

        if (chatClient != null) {
            try {
                String prompt = String.format(
                        "Perform a %s analysis on the following data:\n\n%s\n\nProvide insights and key findings.",
                        analysisType, data);
                ChatResponse response = chatClient.call(new Prompt(prompt));
                result.put("content", response.getResult().getOutput().getContent());
                result.put("isError", false);
            } catch (Exception e) {
                result.put("content", "Error analyzing data: " + e.getMessage());
                result.put("isError", true);
            }
        } else {
            // Fallback when AI client is not configured
            result.put("content", String.format(
                    "AI client not configured. Mock %s analysis for data: %s",
                    analysisType, data.length() > 100 ? data.substring(0, 100) + "..." : data));
            result.put("isError", false);
        }

        result.put("mimeType", "text/markdown");
        return result;
    }

    public Map<String, Object> readResource(String uri) {
        Map<String, Object> result = new HashMap<>();

        switch (uri) {
            case "mcp://server/info":
                result.put("uri", uri);
                result.put("mimeType", "application/json");
                result.put("text",
                        "{\"server\":\"SpringBoot MCP Server\",\"version\":\"1.0.0\",\"capabilities\":[\"tools\",\"resources\"]}");
                break;
            case "mcp://tools/examples":
                result.put("uri", uri);
                result.put("mimeType", "text/markdown");
                result.put("text", getToolExamples());
                break;
            default:
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
                """;
    }
}
