package com.example.mcpserver.service;

import com.example.mcpserver.model.Tool;
import com.example.mcpserver.model.Resource;
import com.example.mcpserver.model.ResourceTemplate;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpService {

    private static final Logger logger = LoggerFactory.getLogger(McpService.class);

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired
    private ApplicationContext applicationContext;

    // Track running operations that can be cancelled
    private final Map<Object, Boolean> runningOperations = new ConcurrentHashMap<>();

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
                ChatResponse response = chatClient.call(new org.springframework.ai.chat.prompt.Prompt(prompt));
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
                ChatResponse response = chatClient.call(new org.springframework.ai.chat.prompt.Prompt(prompt));
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

        result.put("completions", completions);
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
}
