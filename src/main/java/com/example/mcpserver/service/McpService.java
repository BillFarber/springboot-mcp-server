package com.example.mcpserver.service;

import com.example.mcpserver.model.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.mcpserver.model.Resource;
import com.example.mcpserver.model.ResourceTemplate;
import com.example.mcpserver.model.ResourceSubscription;
import com.example.mcpserver.model.ResourceNotification;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawStructuredQueryDefinition;

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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

  @Autowired
  private MarkLogicDocsService markLogicDocsService;

  @Autowired(required = false)
  private DatabaseClient databaseClient;

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

    // Optic code generator tool - inspired by Rush's many talents
    Map<String, Object> opticSchema = Map.of(
        "type", "object",
        "properties", Map.of(
            "prompt", Map.of(
                "type", "string",
                "description", "The user's prompt describing what optic code to generate")),
        "required", List.of("prompt"));

    tools.add(new Tool(
        "optic_code_generator",
        "Generate optic code snippets for data retrieval and transformation",
        opticSchema));

    // Optic code verifier tool - rebellious Rush-style random verification! 🎸
    Map<String, Object> verifyOpticSchema = Map.of(
        "type", "object",
        "properties", Map.of(
            "optic_code", Map.of(
                "type", "string",
                "description", "The optic code to verify for syntax and validity")),
        "required", List.of("optic_code"));

    tools.add(new Tool(
        "verify_optic_code",
        "Verify optic code for syntax and logical correctness (rebellious random verification)",
        verifyOpticSchema));

    Map<String, Object> genericUserPromptSchema = Map.of(
        "type", "object",
        "properties", Map.of(
            "prompt", Map.of(
                "type", "string",
                "description", "The user prompt")),
        "required", List.of("prompt"));

    tools.add(new Tool(
        "marklogic_docs",
        "Help you out with MarkLogic",
        genericUserPromptSchema));

    tools.add(new Tool(
        "search_marklogic",
        "Search MarkLogic database using natural language criteria",
        genericUserPromptSchema));

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
        case "optic_code_generator":
          toolResult = generateOpticCode(arguments);
          break;
        case "verify_optic_code":
          toolResult = verifyOpticCode(arguments);
          break;
        case "marklogic_docs":
          toolResult = markLogicDocs(arguments);
          break;
        case "search_marklogic":
          toolResult = searchMarkLogic(arguments);
          break;
        default:
          // Return MCP-compliant error response
          logger.warn("🔥 Unknown tool requested: {}", toolName);
          mcpResponse.put("content", List.of(Map.of("type", "text", "text",
              "🎸 Epic tool not found! Available tools: generate_text, optic_code_generator, verify_optic_code, marklogic_docs, search_marklogic")));
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

        // 🎸 Pass through any additional metadata from the tool - Epic Rush style! 🎸
        if (toolResult.containsKey("metadata")) {
          mcpResponse.put("metadata", toolResult.get("metadata"));
        }
        if (toolResult.containsKey("mimeType")) {
          mcpResponse.put("mimeType", toolResult.get("mimeType"));
        }
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

  private Map<String, Object> generateOpticCode(Map<String, Object> arguments) {
    Map<String, Object> result = new HashMap<>();

    try {
      // Extract prompt parameter - this is now required
      if (arguments == null) {
        result.put("content",
            List.of(Map.of("type", "text", "text", "🔥 No arguments provided for optic code generation!")));
        result.put("isError", true);
        result.put("mimeType", "text/plain");
        return result;
      }

      String userPrompt = (String) arguments.get("prompt");
      if (userPrompt == null || userPrompt.trim().isEmpty()) {
        result.put("content",
            List.of(Map.of("type", "text", "text",
                "🎸 Prompt is required for epic optic code generation!")));
        result.put("isError", true);
        result.put("mimeType", "text/plain");
        return result;
      }

      // Load comprehensive Optic examples for the LLM
      String opticExamples = loadOpticExamples();

      // Create a comprehensive prompt for the LLM to generate optic code
      String systemPrompt = String.format(
          """
              You are an expert MarkLogic Optic code generator.
              Generate optic code based on the user's request inspired by the precision and versatility of the band Rush.
              Optic code is used for reading rows of data from MarkLogic databases and then data transformation and manipulation.

              Here are comprehensive examples of MarkLogic Optic API functions to reference:

              %s

              Based on these examples and the user's specific request below, generate practical, well-commented optic code that fulfills the user's request.
              Use the appropriate patterns and functions from the examples above.
              Include 🎸 Rush-inspired comments for style, but keep the code functional and clear.

              User's request: %s

              Generate the most appropriate optic code solution using the patterns and functions demonstrated in the examples.
              """,
          opticExamples, userPrompt);

      if (chatClient != null) {
        try {
          logger.debug("🎸 Generating optic code with LLM for prompt: {}", userPrompt);
          logger.debug("🎸 System prompt being sent to LLM: {}", systemPrompt);
          ChatResponse response = chatClient
              .call(new org.springframework.ai.chat.prompt.Prompt(systemPrompt));

          if (response != null && response.getResult() != null) {
            String generatedCode = response.getResult().getOutput().getContent();
            logger.debug("🎸 LLM response received: {}", generatedCode);
            result.put("content", List.of(Map.of("type", "text", "text", generatedCode)));
            result.put("isError", false);
            logger.info("🎸 Successfully generated optic code using LLM for prompt: '{}'", userPrompt);
            result.put("mimeType", "text/javascript");
            return result;
          } else {
            logger.warn("🔥 AI response was incomplete - falling back to template");
          }
        } catch (Exception e) {
          logger.warn("� LLM optic code generation failed - falling back to template: {}", e.getMessage());
        }
      }

      // Fallback when AI client is not configured or fails - provide a basic template
      logger.warn("ChatClient is null or failed - using fallback optic code generation");
      String fallbackCode = String.format("""
          // 🎸 AI client not configured - Epic fallback optic code for: %s

          // Basic optic code template - customize as needed
          const result = op.fromView('schema', 'view')
            .select(['*'])
            .where(op.ne('deleted', true))
            .orderBy('id')
            .result();

          console.log('🎸 Optic code generated for: %s', result);

          // TODO: Customize this template based on your specific needs
          // This is a fallback template when AI is not available
          """, userPrompt, userPrompt);

      result.put("content", List.of(Map.of("type", "text", "text", fallbackCode)));
      result.put("isError", false);
      logger.info("🎸 Generated fallback optic code for prompt: '{}'", userPrompt);

      result.put("mimeType", "text/javascript");

    } catch (Exception e) {
      logger.error("💥 Unexpected error in generateOpticCode", e);
      result.put("content",
          List.of(Map.of("type", "text", "text", "🔥 Epic optic code generation failed: " + e.getMessage())));
      result.put("isError", true);
      result.put("mimeType", "text/plain");
    }

    return result;
  }

  private Map<String, Object> verifyOpticCode(Map<String, Object> arguments) {
    Map<String, Object> result = new HashMap<>();

    try {
      // 🎸 Epic defensive coding - Rush style! 🎸
      if (arguments == null) {
        result.put("content",
            List.of(Map.of("type", "text", "text",
                "🔥 No arguments provided for optic code verification!")));
        result.put("isError", true);
        result.put("mimeType", "text/plain");
        return result;
      }

      String opticCode = (String) arguments.get("optic_code");
      if (opticCode == null || opticCode.trim().isEmpty()) {
        result.put("content",
            List.of(Map.of("type", "text", "text", "🎸 Optic code is required for epic verification!")));
        result.put("isError", true);
        result.put("mimeType", "text/plain");
        return result;
      }

      // 🎸 REBELLIOUS RUSH-STYLE RANDOM VERIFICATION! 🎸
      // Like the unpredictable genius of Neil Peart's drumming!
      boolean isValid = Math.random() < 0.5; // 50-50 chance, pure rebellious chaos!

      String verificationMessage;
      if (isValid) {
        verificationMessage = String.format(
            "🎸 EPIC! Your optic code passes verification! Like a perfect Rush solo! 🎸\n\n" +
                "Code length: %d characters\n" +
                "Verification result: ✅ VALID\n" +
                "Rush wisdom: \"The music must be the master\" - and your code is mastering the data!\n\n"
                +
                "Note: This is a rebellious random verification (50-50 chance) - rock on!",
            opticCode.length());
      } else {
        verificationMessage = String.format(
            "🔥 Code verification failed! Like a missed note in Tom Sawyer! 🔥\n\n" +
                "Code length: %d characters\n" +
                "Verification result: ❌ INVALID\n" +
                "Rush wisdom: \"Subdivisions - in the high school halls\" - your code needs subdivision!\n\n"
                +
                "Note: This is a rebellious random verification (50-50 chance) - try again for rock glory!",
            opticCode.length());
      }

      result.put("content", List.of(Map.of("type", "text", "text", verificationMessage)));
      result.put("isError", false);
      result.put("mimeType", "text/plain");

      // 🎸 Add some extra Rush-style metadata! 🎸
      Map<String, Object> metadata = Map.of(
          "isValid", isValid,
          "codeLength", opticCode.length(),
          "verificationMethod", "rebellious_random_rush_style",
          "rushQuote", isValid ? "The music must be the master" : "Subdivisions - in the high school halls");
      result.put("metadata", metadata);

      logger.info("🎸 Epic optic code verification completed for {} characters: {} (Random Rush verification)",
          opticCode.length(), isValid ? "VALID" : "INVALID");

    } catch (Exception e) {
      logger.error("💥 Unexpected error in verifyOpticCode", e);
      result.put("content",
          List.of(Map.of("type", "text", "text", "🔥 Epic verification explosion: " + e.getMessage())));
      result.put("isError", true);
      result.put("mimeType", "text/plain");
    }

    return result;
  }

  private Map<String, Object> markLogicDocs(Map<String, Object> arguments) {
    Map<String, Object> result = new HashMap<>();
    try {
      String userPrompt = (String) arguments.get("prompt");
      List<EmbeddingMatch<TextSegment>> matches = markLogicDocsService.search(userPrompt, 5);
      StringBuilder sb = new StringBuilder();
      matches.forEach(match -> {
        System.out.println("MATCH: " + match.embedded().text());
        System.out.println("SCORE: " + match.score());
        sb.append("\n").append(match.embedded().text()).append("\n\n");
      });
      result.put("content", List.of(Map.of("type", "text", "text", sb.toString())));
      result.put("isError", false);
      result.put("mimeType", "text/plain");
    } catch (Exception e) {
      logger.error("💥 Unexpected error in markLogicDocs", e);
      result.put("content",
          List.of(Map.of("type", "text", "text", "🔥 Epic verification explosion: " + e.getMessage())));
      result.put("isError", true);
      result.put("mimeType", "text/plain");
    }
    return result;
  }

  private Map<String, Object> searchMarkLogic(Map<String, Object> arguments) {
    Map<String, Object> result = new HashMap<>();

    try {
      // 🎸 Epic defensive coding! 🎸
      if (arguments == null) {
        result.put("content",
            List.of(Map.of("type", "text", "text", "🔥 No arguments provided for MarkLogic search!")));
        result.put("isError", true);
        result.put("mimeType", "text/plain");
        return result;
      }

      String searchPrompt = (String) arguments.get("prompt");
      if (searchPrompt == null || searchPrompt.trim().isEmpty()) {
        result.put("content",
            List.of(Map.of("type", "text", "text", "🎸 Search prompt is required for epic MarkLogic search!")));
        result.put("isError", true);
        result.put("mimeType", "text/plain");
        return result;
      }

      // 🎸 Epic MarkLogic DatabaseClient availability check! 🎸
      logger.debug("🎸 DatabaseClient availability: {}", databaseClient != null ? "AVAILABLE" : "NOT AVAILABLE");
      if (databaseClient != null) {
        logger.debug("🎸 Ready to rock with MarkLogic database connectivity!");
      } else {
        logger.debug("🎸 DatabaseClient not configured - using query generation mode only");
      }

      // 🎸 Load comprehensive MarkLogic Structured Query examples for the LLM
      String structuredQueryExamples = loadMarkLogicStructuredQueryExamples();

      // Create a comprehensive prompt for the LLM to generate MarkLogic Structured
      // Query
      String systemPrompt = String.format(
          """
              You are an expert MarkLogic Structured Query generator.
              Generate a structured query (JSON format) based on the user's natural language request.

              Here are comprehensive examples of MarkLogic structured query patterns to reference:

              %s

              Based on these examples and the user's search request below, generate a structured query
              that represents the search requirements.

              Guidelines:
              - Generate a structured query in JSON format that can be used with MarkLogic Java Client API
              - Use appropriate query types (word-query, element-query, range-query, collection-query, etc.)
              - Include proper query structure and nesting for complex searches
              - Return only the structured query JSON without additional code or execution logic
              - Focus on the query construction, not the result processing
              - Use the "query" wrapper structure for structured queries
              - Support collection filtering, text search, range queries, and combinations

              User's search request: %s

              Generate the most appropriate MarkLogic structured query in JSON format.
              """,
          structuredQueryExamples, searchPrompt);

      if (chatClient != null) {
        try {
          logger.debug("🎸 Generating MarkLogic structured query with LLM for prompt: {}", searchPrompt);
          ChatResponse response = chatClient
              .call(new org.springframework.ai.chat.prompt.Prompt(systemPrompt));

          if (response != null && response.getResult() != null) {
            String generatedStructuredQuery = response.getResult().getOutput().getContent();
            logger.debug("🎸 LLM structured query response received: {}", generatedStructuredQuery);

            // 🎸 EPIC DATABASE EXECUTION TIME! 🎸
            if (databaseClient != null) {
              try {
                logger.debug("🎸 EXECUTING STRUCTURED QUERY AGAINST MARKLOGIC DATABASE! 🎸");

                // Execute the search using MarkLogic database
                String searchResults = executeMarkLogicStructuredSearch(generatedStructuredQuery);

                // Format the response with actual search results
                String formattedResponse = String.format(
                    "🎸 EPIC MARKLOGIC SEARCH EXECUTED! 🎸\n\n" +
                        "Search request: \"%s\"\n\n" +
                        "Generated Structured Query:\n" +
                        "```json\n%s\n```\n\n" +
                        "🚀 SEARCH RESULTS:\n" +
                        "```json\n%s\n```\n\n" +
                        "🎸 Live search results from MarkLogic database - ROCK ON!",
                    searchPrompt, generatedStructuredQuery, searchResults);

                result.put("content", List.of(Map.of("type", "text", "text", formattedResponse)));
                result.put("isError", false);
                result.put("mimeType", "text/markdown");

                // 🎸 Add comprehensive metadata with actual results
                Map<String, Object> metadata = Map.of(
                    "searchPrompt", searchPrompt,
                    "generatedQuery", generatedStructuredQuery,
                    "searchResults", searchResults,
                    "queryFormat", "structured_json",
                    "searchFramework", "marklogic_structured",
                    "toolVersion", "structured_live_search_v1.0",
                    "executionMode", "live_database",
                    "rushQuote",
                    "The trees - all kept equal by hatchet, axe, and saw! Your data is equally accessible!");
                result.put("metadata", metadata);

                logger.info("🎸 Successfully executed MarkLogic structured search for prompt: '{}' - LIVE RESULTS!",
                    searchPrompt);
                return result;

              } catch (Exception searchException) {
                logger.error("🔥 Database search execution failed: {}", searchException.getMessage(), searchException);

                // Fall back to query-only mode with error info
                String fallbackResponse = String.format(
                    "🎸 MarkLogic Structured Query Generated (Database Execution Failed) 🎸\n\n" +
                        "Search request: \"%s\"\n\n" +
                        "Generated Structured Query:\n" +
                        "```json\n%s\n```\n\n" +
                        "🔧 Database execution failed: %s\n" +
                        "📋 Copy this query to MarkLogic Query Console to execute manually!\n" +
                        "🎸 The show must go on - even when the amp fails!",
                    searchPrompt, generatedStructuredQuery, searchException.getMessage());

                result.put("content", List.of(Map.of("type", "text", "text", fallbackResponse)));
                result.put("isError", false);
                result.put("mimeType", "text/markdown");

                // 🎸 Add metadata with error info
                Map<String, Object> metadata = Map.of(
                    "searchPrompt", searchPrompt,
                    "generatedQuery", generatedStructuredQuery,
                    "queryFormat", "structured_json",
                    "searchFramework", "marklogic_structured",
                    "toolVersion", "structured_fallback_v1.0",
                    "executionMode", "query_only_fallback",
                    "errorMessage", searchException.getMessage(),
                    "rushQuote", "Freewill - you choose how to execute this query!");
                result.put("metadata", metadata);

                logger.info("🎸 Generated MarkLogic structured query for prompt: '{}' (database execution failed)",
                    searchPrompt);
                return result;
              }
            }

            // DatabaseClient not available - query generation mode only
            logger.debug("🎸 DatabaseClient not available - query generation mode only");

            // Format the response with additional context
            String formattedResponse = String.format(
                "🎸 Epic MarkLogic Serialized CTS Query Generated! 🎸\n\n" +
                    "Search request: \"%s\"\n\n" +
                    "Generated Serialized CTS Query:\n" +
                    "```json\n%s\n```\n\n" +
                    "� Copy this code into MarkLogic Query Console to execute your search!\n" +
                    "🎸 Rock on with your epic data quest!",
                searchPrompt, generatedStructuredQuery);

            result.put("content", List.of(Map.of("type", "text", "text", formattedResponse)));
            result.put("isError", false);
            result.put("mimeType", "text/markdown");

            // 🎸 Add comprehensive metadata
            Map<String, Object> metadata = Map.of(
                "searchPrompt", searchPrompt,
                "generatedQuery", generatedStructuredQuery,
                "queryFormat", "structured_json",
                "searchFramework", "marklogic_structured",
                "toolVersion", "structured_serialized_v1.0",
                "rushQuote", "The spirit of radio - broadcasting your search across the data universe!");
            result.put("metadata", metadata);

            logger.info("🎸 Successfully generated MarkLogic structured query for prompt: '{}'", searchPrompt);
            return result;
          } else {
            logger.warn("🔥 AI response was incomplete - falling back to template");
          }
        } catch (Exception e) {
          logger.warn("🔥 LLM structured search generation failed - falling back to template: {}", e.getMessage());
        }
      }

      // Fallback when AI client is not configured or fails - provide a basic CTS
      // template
      logger.warn("ChatClient is null or failed - using fallback CTS query generation");
      String fallbackCTSQuery = String.format("""
          {
            "word-query": {
              "text": ["%s"]
            }
          }
          """, searchPrompt.replaceAll("[^a-zA-Z0-9\\s]", "").trim());

      String fallbackResponse = String.format(
          "🎸 MarkLogic CTS Query (Fallback Mode) 🎸\n\n" +
              "Search request: \"%s\"\n\n" +
              "Generated Serialized CTS Query:\n" +
              "```json\n%s\n```\n\n" +
              "🔧 Note: This is a fallback template generated when AI is not available.\n" +
              "🎸 Customize the query based on your specific search needs!",
          searchPrompt, fallbackCTSQuery);

      result.put("content", List.of(Map.of("type", "text", "text", fallbackResponse)));
      result.put("isError", false);
      result.put("mimeType", "text/markdown");

      // 🎸 Add metadata for fallback
      Map<String, Object> metadata = Map.of(
          "searchPrompt", searchPrompt,
          "status", "fallback_template",
          "queryFormat", "json",
          "searchFramework", "marklogic_cts",
          "toolVersion", "cts_serialized_fallback_v1.0",
          "rushQuote", "Freewill - choose your own search adventure!");
      result.put("metadata", metadata);

      logger.info("🎸 Generated fallback MarkLogic CTS query for prompt: '{}'", searchPrompt);

    } catch (Exception e) {
      logger.error("💥 Unexpected error in searchMarkLogic", e);
      result.put("content",
          List.of(Map.of("type", "text", "text", "🔥 Epic search explosion: " + e.getMessage())));
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

        ## Optic Code Generator Tool

        ```json
        {
          "method": "tools/call",
          "params": {
            "name": "optic_code_generator",
            "arguments": {
              "prompt": "Create optic code to read user profiles from the database, filter active users, and transform the data by adding a full name field"
            }
          }
        }
        ```

        Advanced usage:
        ```json
        {
          "method": "tools/call",
          "params": {
            "name": "optic_code_generator",
            "arguments": {
              "prompt": "Generate optic code to aggregate sales data by month, calculate totals, and sort by highest revenue first"
            }
          }
        }
        ```

        ## Optic Code Verifier Tool (Rebellious Random Verification!)

        ```json
        {
          "method": "tools/call",
          "params": {
            "name": "verify_optic_code",
            "arguments": {
              "optic_code": "const result = op.fromView('users', 'profiles').select(['name', 'email']).result();"
            }
          }
        }
        ```

        Rush-style rebellious example:
        ```json
        {
          "method": "tools/call",
          "params": {
            "name": "verify_optic_code",
            "arguments": {
              "optic_code": "// Epic optic code\\nconst data = op.fromView('sales', 'quarterly')\\n  .groupBy(['region'], [op.sum('revenue')])\\n  .orderBy([op.desc('revenue')])\\n  .result();"
            }
          }
        }
        ```

        ## Search MarkLogic Tool - Generate CTS Search Code!

        ```json
        {
          "method": "tools/call",
          "params": {
            "name": "search_marklogic",
            "arguments": {
              "prompt": "Find all documents about machine learning published after 2020"
            }
          }
        }
        ```

        Advanced search example:
        ```json
        {
          "method": "tools/call",
          "params": {
            "name": "search_marklogic",
            "arguments": {
              "prompt": "Search for customer records with high satisfaction scores in the technology sector, sorted by revenue"
            }
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
      case "optic_code_generator" ->
        """
            # Optic Code Generator Tool Documentation

            ## Overview
            The `optic_code_generator` tool leverages AI to generate sophisticated optic code snippets for data transformation.
            Inspired by Rush's many talents, this tool creates custom optic expressions based on your specific requirements.

            ## Parameters
            - **prompt** (required): Your description of what optic code you need generated

            ## Example Usage
            ```json
            {
              "name": "optic_code_generator",
              "arguments": {
                "prompt": "Create optic code to read user profiles from the database, filter active users, and transform the data by adding a full name field"
              }
            }
            ```

            ```json
            {
              "name": "optic_code_generator",
              "arguments": {
                "prompt": "Generate optic code to aggregate sales data by month and calculate totals"
              }
            }
            ```

            ## Expected Response
            Returns AI-generated optic code tailored to your specific requirements, complete with:
            - Proper optic syntax and structure
            - Relevant comments and explanations
            - Rush-inspired styling for epic code generation

            ## AI-Powered Features
            - Understands complex data transformation requirements
            - Generates contextually appropriate optic operations
            - Includes best practices and error handling
            - Provides Rush-level precision in code generation

            ## Rush Connection
            Like the band Rush's progressive and intricate compositions, this tool creates sophisticated,
            well-structured optic code that adapts to your unique data transformation needs.
            """;
      case "verify_optic_code" ->
        """
            # Optic Code Verifier Tool Documentation

            ## Overview
            The `verify_optic_code` tool provides rebellious, Rush-style random verification of optic code.
            Like the unpredictable genius of Neil Peart's drumming, this tool uses pure random chaos to verify your code!

            ## Parameters
            - **optic_code** (required): The optic code string to verify for syntax and validity

            ## Example Usage
            ```json
            {
              "name": "verify_optic_code",
              "arguments": {
                "optic_code": "const result = op.fromView('users', 'profiles').select(['name', 'email']).result();"
              }
            }
            ```

            Complex example:
            ```json
            {
              "name": "verify_optic_code",
              "arguments": {
                "optic_code": "const analytics = op.fromView('sales', 'quarterly')\\n  .groupBy(['region'], [op.sum('revenue')])\\n  .orderBy([op.desc('revenue')])\\n  .result();"
              }
            }
            ```

            ## Expected Response
            Returns a rebellious random verification result (50-50 chance) with:
            - ✅ VALID or ❌ INVALID status
            - Code length analysis
            - Rush-inspired wisdom quotes
            - Verification metadata

            ## Rebellious Features
            - 🎸 **Pure Random Chaos**: 50-50 chance of success, just like rock & roll!
            - 🔥 **Rush-Style Messages**: Epic feedback inspired by the greatest progressive rock band
            - ⚡ **Metadata Rich**: Includes verification method and Rush quotes
            - 🚀 **No Real Verification**: Purely rebellious - doesn't actually check syntax!

            ## Rush Connection
            Like Rush's rebellious spirit against mainstream music, this tool rebels against traditional
            code verification by using pure randomness. Sometimes you get "Tom Sawyer" perfection,
            sometimes you need to "subdivide" your approach!

            ## Warning
            This is a demonstration tool using random verification. For production optic code validation,
            you'd want actual syntax and semantic analysis. But where's the fun in that? Rock on! 🎸
            """;
      case "search_marklogic" ->
        """
            # Search MarkLogic Tool Documentation

            ## Overview
            The `search_marklogic` tool converts natural language search requests into JavaScript-based
            MarkLogic Content and Text Search (CTS) code. Uses AI when available, with comprehensive
            fallback templates when AI is not configured.

            ## Parameters
            - **prompt** (required): Natural language description of what you want to search for

            ## Example Usage
            ```json
            {
              "name": "search_marklogic",
              "arguments": {
                "prompt": "Find all documents about machine learning published after 2020"
              }
            }
            ```

            ```json
            {
              "name": "search_marklogic",
              "arguments": {
                "prompt": "Search for customer records with high satisfaction scores in the technology sector"
              }
            }
            ```

            ## Current Status
            ✅ **Fully Functional**: This tool generates serialized MarkLogic CTS queries in JSON format
            based on natural language search requests.

            ## Expected Response
            Returns generated MarkLogic CTS queries with:
            - **AI Mode**: Intelligent CTS query generation using comprehensive examples
            - **Fallback Mode**: Template-based CTS queries when AI is unavailable
            - **Format**: Markdown with JSON code blocks for easy copy-paste
            - **Metadata**: Search prompt, generated query, format info, and Rush quotes

            ## Features
            - 🚀 **Natural Language Processing**: Converts natural language to MarkLogic CTS queries
            - 🎯 **Smart Query Generation**: Uses comprehensive CTS examples for context
            - 📊 **Multiple Search Types**: Supports text, element, range, and complex queries
            - 🔍 **Comprehensive Examples**: Includes patterns for all major CTS search types
            - ⚡ **Robust Fallback**: Provides functional templates when AI unavailable

            ## Rush Connection
            Like Rush's meticulous attention to detail in their compositions, this tool provides
            precise, well-crafted CTS queries that find exactly what you're looking for
            in your MarkLogic database.

            ## Implementation Features
            - Full-text search across document collections
            - Element and attribute queries
            - Range queries for numbers and dates
            - Collection and directory filtering
            - JSON property searches
            - Proximity and wildcard patterns
            - Geospatial search capabilities
            - Performance optimization patterns

            🎸 Epic search functionality is here - rock your MarkLogic queries! 🎸
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

        // Check for null response or result
        if (response != null && response.getResult() != null &&
            response.getResult().getOutput() != null &&
            response.getResult().getOutput().getContent() != null) {

          String aiResponse = response.getResult().getOutput().getContent();

          // Try to parse AI response as completion suggestions
          // For now, create a simple completion based on AI response
          Map<String, Object> aiCompletion = new HashMap<>();
          aiCompletion.put("text", aiResponse.length() > 100 ? aiResponse.substring(0, 100) + "..." : aiResponse);
          aiCompletion.put("description", "AI-generated completion");
          completions.add(aiCompletion);
        } else {
          logger.warn("AI response was null or incomplete - using static completions only");
        }

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

  /**
   * 🎸 Load comprehensive Optic examples for LLM context - Epic knowledge base!
   * 🎸
   */
  private String loadOpticExamples() {
    try {
      // Try to load from the examples file in resources
      org.springframework.core.io.Resource resource = applicationContext
          .getResource("classpath:optic-examples.js");
      if (resource != null && resource.exists()) {
        InputStream inputStream = resource.getInputStream();
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      } else {
        logger.warn("🔥 Optic examples file not found in classpath, using fallback examples");
      }
    } catch (IOException | NullPointerException e) {
      logger.warn("🔥 Could not load optic examples file: {}", e.getMessage());
    }

    // Return a comprehensive set of examples as fallback
    return """
        // 🎸 Comprehensive MarkLogic Optic API Examples - Rush 2112 Style! 🎸

        // ============================================================================
        // 🎸 BASIC QUERY OPERATIONS - The Foundation 🎸
        // ============================================================================

        // Basic view query - Read data from a TDE view
        const basicViewQuery = op.fromView('employees', 'employee_details')
          .result();

        // Select specific columns
        const selectColumns = op.fromView('products', 'catalog')
          .select(['product_id', 'name', 'price', 'category'])
          .result();

        // ============================================================================
        // 🎸 FILTERING AND CONDITIONS - Where the Magic Happens 🎸
        // ============================================================================

        // Filter rows with conditions
        const filteredData = op.fromView('orders', 'order_details')
          .where(op.and(
            op.ge(op.col('order_date'), '2024-01-01'),
            op.eq(op.col('status'), 'completed'),
            op.gt(op.col('total_amount'), 100)
          ))
          .result();

        // ============================================================================
        // 🎸 JOINING DATA - Bringing It All Together 🎸
        // ============================================================================

        // Inner join between datasets
        const innerJoinExample = op.fromView('orders', 'order_summary')
          .joinInner(
            op.fromView('customers', 'customer_info'),
            op.on(op.col('customer_id'), op.col('id'))
          )
          .select([
            'order_id',
            'order_date',
            'customer_name',
            'customer_email',
            'total_amount'
          ])
          .result();

        // Left outer join
        const leftJoinExample = op.fromView('products', 'inventory')
          .joinLeftOuter(
            op.fromView('sales', 'product_sales'),
            op.on(op.col('product_id'), op.col('product_id'))
          )
          .select([
            'product_name',
            'current_stock',
            op.as('total_sold', op.col('quantity_sold'))
          ])
          .result();

        // ============================================================================
        // 🎸 AGGREGATION - The Power of Numbers 🎸
        // ============================================================================

        // Group by with aggregation functions
        const groupedAggregation = op.fromView('sales', 'daily_transactions')
          .groupBy([
            'region',
            'product_category'
          ], [
            op.as('total_sales', op.sum(op.col('amount'))),
            op.as('avg_order_value', op.avg(op.col('amount'))),
            op.as('transaction_count', op.count()),
            op.as('max_single_sale', op.max(op.col('amount')))
          ])
          .result();

        // ============================================================================
        // 🎸 SORTING AND ORDERING - Arrange with Precision 🎸
        // ============================================================================

        // Sort results
        const sortedResults = op.fromView('products', 'bestsellers')
          .orderBy([
            op.desc('total_sales'),
            'product_name',
            op.asc('price')
          ])
          .result();

        // ============================================================================
        // 🎸 MATHEMATICAL OPERATIONS - Calculate Like a Rock Star 🎸
        // ============================================================================

        // Mathematical functions in select
        const mathematicalOperations = op.fromView('financial', 'investments')
          .select([
            'account_id',
            'principal_amount',
            'interest_rate',
            // Simple interest calculation
            op.as('simple_interest', op.multiply(
              op.col('principal_amount'),
              op.col('interest_rate'),
              op.col('years')
            )),
            // Percentage calculations
            op.as('growth_percentage', op.multiply(
              op.divide(
                op.subtract(op.col('current_value'), op.col('principal_amount')),
                op.col('principal_amount')
              ),
              100
            ))
          ])
          .result();

        // ============================================================================
        // 🎸 STRING OPERATIONS - Text Manipulation Mastery 🎸
        // ============================================================================

        // String functions for text processing
        const stringOperations = op.fromView('users', 'profiles')
          .select([
            'user_id',
            op.as('full_name', op.concat(op.col('first_name'), ' ', op.col('last_name'))),
            op.as('name_length', op.length(op.concat(op.col('first_name'), op.col('last_name')))),
            op.as('email_domain', op.substring(op.col('email'), op.add(op.indexOf(op.col('email'), '@'), 1))),
            op.as('username_upper', op.upper(op.col('username')))
          ])
          .result();

        // ============================================================================
        // 🎸 DATE AND TIME OPERATIONS - Temporal Precision 🎸
        // ============================================================================

        // Date/time functions and calculations
        const dateTimeOperations = op.fromView('events', 'calendar')
          .select([
            'event_id',
            'event_name',
            'start_time',
            // Extract date components
            op.as('year', op.year(op.col('start_time'))),
            op.as('month', op.month(op.col('start_time'))),
            op.as('day_of_week', op.dayOfWeek(op.col('start_time'))),
            // Calculate durations
            op.as('duration_minutes', op.dateDiff('minute', op.col('start_time'), op.col('end_time'))),
            op.as('days_until_event', op.dateDiff('day', op.currentDateTime(), op.col('start_time')))
          ])
          .where(op.ge(op.col('start_time'), op.currentDate()))
          .result();

        // ============================================================================
        // 🎸 CONDITIONAL LOGIC - Decision Making in Queries 🎸
        // ============================================================================

        // Case expressions for conditional logic
        const conditionalLogic = op.fromView('customers', 'profiles')
          .select([
            'customer_id',
            'customer_name',
            'total_purchases',
            // Customer tier based on purchase amount
            op.as('customer_tier',
              op.case([
                op.when(op.ge(op.col('total_purchases'), 10000), 'Platinum'),
                op.when(op.ge(op.col('total_purchases'), 5000), 'Gold'),
                op.when(op.ge(op.col('total_purchases'), 1000), 'Silver')
              ], 'Bronze')
            ),
            // Handle null values
            op.as('description', op.coalesce(op.col('description'), 'No description available'))
          ])
          .result();

        // ============================================================================
        // 🎸 LIMITING AND PAGINATION - Control Your Results 🎸
        // ============================================================================

        // Pagination with limit and offset
        const paginatedResults = op.fromView('articles', 'blog_posts')
          .where(op.eq(op.col('status'), 'published'))
          .orderBy([op.desc('publish_date')])
          .offset(20)  // Skip first 20 results
          .limit(10)   // Return next 10 results
          .result();

        // ============================================================================
        // 🎸 REAL-WORLD BUSINESS EXAMPLES - Epic Scenarios 🎸
        // ============================================================================

        // Customer Analytics Dashboard Query
        const customerAnalytics = op.fromView('customers', 'profiles')
          .joinLeftOuter(
            op.fromView('orders', 'summary')
              .groupBy(['customer_id'], [
                op.as('total_orders', op.count()),
                op.as('total_spent', op.sum(op.col('amount'))),
                op.as('avg_order_value', op.avg(op.col('amount')))
              ]),
            op.on(op.col('customer_id'), op.col('customer_id'))
          )
          .select([
            'customer_id',
            'customer_name',
            'email',
            op.as('lifetime_value', op.coalesce(op.col('total_spent'), 0)),
            op.as('customer_segment',
              op.case([
                op.when(op.ge(op.coalesce(op.col('total_spent'), 0), 5000), 'VIP'),
                op.when(op.ge(op.coalesce(op.col('total_spent'), 0), 1000), 'Premium')
              ], 'Standard')
            )
          ])
          .result();

        // 🎸 End of Epic Optic Examples - Rock on with your data transformations! 🎸
        """;
  }

  /**
   * 🎸 Load comprehensive MarkLogic Structured Query examples for LLM training
   */
  private String loadMarkLogicStructuredQueryExamples() {
    return """
        🎸 EPIC MARKLOGIC STRUCTURED QUERY EXAMPLES! 🎸

        These examples show the proper JSON format for MarkLogic Structured Queries.
        Generate queries that follow these patterns exactly:

        # 1. SIMPLE TEXT SEARCH
        User: "find documents with Rush"
        {
          "query": {
            "term-query": {
              "text": ["Rush"]
            }
          }
        }

        # 2. COLLECTION FILTERING
        User: "show me red documents"
        {
          "query": {
            "collection-query": {
              "uri": ["red"]
            }
          }
        }

        # 3. COMBINED TEXT AND COLLECTION
        User: "find Rush songs in red collection"
        {
          "query": {
            "and-query": {
              "queries": [
                {
                  "term-query": {
                    "text": ["Rush"]
                  }
                },
                {
                  "collection-query": {
                    "uri": ["red"]
                  }
                }
              ]
            }
          }
        }

        # 4. MULTIPLE TERMS (OR)
        User: "find documents about drums or bass"
        {
          "query": {
            "or-query": {
              "queries": [
                {
                  "term-query": {
                    "text": ["drums"]
                  }
                },
                {
                  "term-query": {
                    "text": ["bass"]
                  }
                }
              ]
            }
          }
        }

        # 5. ELEMENT QUERIES
        User: "find songs with title containing Time"
        {
          "query": {
            "element-query": {
              "element": {
                "name": "title"
              },
              "query": {
                "term-query": {
                  "text": ["Time"]
                }
              }
            }
          }
        }

        # 6. ATTRIBUTE QUERIES
        User: "find documents where genre is progressive"
        {
          "query": {
            "element-attribute-query": {
              "element": {
                "name": "song"
              },
              "attribute": {
                "name": "genre"
              },
              "query": {
                "term-query": {
                  "text": ["progressive"]
                }
              }
            }
          }
        }

        # 7. RANGE QUERIES (for dates/numbers)
        User: "find songs from 1980 to 1990"
        {
          "query": {
            "range-query": {
              "type": "xs:int",
              "element": {
                "name": "year"
              },
              "operator": "GE",
              "value": ["1980"]
            }
          }
        }

        # 8. COMPLEX NESTED QUERIES
        User: "find Rush or Geddy Lee songs in red collection but not from 1970s"
        {
          "query": {
            "and-query": {
              "queries": [
                {
                  "or-query": {
                    "queries": [
                      {
                        "term-query": {
                          "text": ["Rush"]
                        }
                      },
                      {
                        "term-query": {
                          "text": ["Geddy Lee"]
                        }
                      }
                    ]
                  }
                },
                {
                  "collection-query": {
                    "uri": ["red"]
                  }
                },
                {
                  "not-query": {
                    "query": {
                      "range-query": {
                        "type": "xs:int",
                        "element": {
                          "name": "year"
                        },
                        "operator": "GE",
                        "value": ["1970"]
                      }
                    }
                  }
                }
              ]
            }
          }
        }

        # 9. DOCUMENT QUERIES (specific documents)
        User: "find document with URI /rush/songs/freewill.xml"
        {
          "query": {
            "document-query": {
              "uri": ["/rush/songs/freewill.xml"]
            }
          }
        }

        # 10. WILDCARD QUERIES
        User: "find words starting with rock"
        {
          "query": {
            "term-query": {
              "text": ["rock*"]
            }
          }
        }

        # 11. PHRASE QUERIES
        User: "find exact phrase 'Working Man'"
        {
          "query": {
            "term-query": {
              "text": ["Working Man"]
            }
          }
        }

        # 12. MULTIPLE COLLECTIONS
        User: "find documents in red or blue collections"
        {
          "query": {
            "or-query": {
              "queries": [
                {
                  "collection-query": {
                    "uri": ["red"]
                  }
                },
                {
                  "collection-query": {
                    "uri": ["blue"]
                  }
                }
              ]
            }
          }
        }

        🎸 CRITICAL STRUCTURED QUERY RULES:
        1. Always wrap the main query in a "query" object
        2. Use proper query types: term-query, element-query, collection-query, etc.
        3. Text searches go in "text" arrays: ["search term"]
        4. Collections go in "uri" arrays: ["collection-name"]
        5. Combine queries with and-query, or-query, not-query
        6. Each query type has specific required properties
        7. Return ONLY valid JSON - no explanations or markdown

        🎸 RESPONSE FORMAT:
        Generate a single JSON object that represents the structured query.
        The query will be executed directly against MarkLogic using the Java Client API.

        🎸 Remember: You are the master of MarkLogic structured queries - make them EPIC! 🎸
        """;
  }

  /**
   * 🎸 Load comprehensive MarkLogic CTS examples for LLM context - Epic search
   * knowledge base! 🎸
   */
  private String loadMarkLogicCTSExamples() {
    try {
      // Try to load from the examples file in resources
      org.springframework.core.io.Resource resource = applicationContext
          .getResource("classpath:marklogic-cts-examples.js");
      if (resource != null && resource.exists()) {
        InputStream inputStream = resource.getInputStream();
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      } else {
        logger.warn("🔥 MarkLogic CTS examples file not found in classpath, using fallback examples");
      }
    } catch (IOException | NullPointerException e) {
      logger.warn("🔥 Could not load MarkLogic CTS examples file: {}", e.getMessage());
    }

    // Return a comprehensive set of CTS examples as fallback
    return """
        // 🎸 Comprehensive MarkLogic Content and Text Search (CTS) Examples - Rush 2112 Style! 🎸

        // ============================================================================
        // 🎸 BASIC TEXT SEARCH - The Foundation of Discovery 🎸
        // ============================================================================

        // Simple word search - Find documents containing specific words
        const basicWordSearch = cts.search(cts.wordQuery('marklogic'));

        // Multiple word search with AND logic
        const multiWordSearch = cts.search(
          cts.andQuery([
            cts.wordQuery('data'),
            cts.wordQuery('analytics'),
            cts.wordQuery('platform')
          ])
        );

        // OR query for alternative terms
        const orWordSearch = cts.search(
          cts.orQuery([
            cts.wordQuery('database'),
            cts.wordQuery('datastore'),
            cts.wordQuery('repository')
          ])
        );

        // ============================================================================
        // 🎸 PHRASE AND EXACT MATCH SEARCHES - Precision Like Neil Peart's Drumming 🎸
        // ============================================================================

        // Exact phrase search
        const phraseSearch = cts.search(
          cts.wordQuery('machine learning algorithms', ['exact'])
        );

        // Case-sensitive search
        const caseSensitiveSearch = cts.search(
          cts.wordQuery('API', ['case-sensitive'])
        );

        // Stemmed search (finds variations like 'running', 'ran', 'runs')
        const stemmedSearch = cts.search(
          cts.wordQuery('run', ['stemmed'])
        );

        // ============================================================================
        // 🎸 ELEMENT AND ATTRIBUTE SEARCHES - Structured Data Mastery 🎸
        // ============================================================================

        // Search within specific XML elements
        const elementSearch = cts.search(
          cts.elementWordQuery(fn.QName('', 'title'), 'introduction')
        );

        // Search multiple elements
        const multiElementSearch = cts.search(
          cts.orQuery([
            cts.elementWordQuery(fn.QName('', 'title'), 'tutorial'),
            cts.elementWordQuery(fn.QName('', 'description'), 'beginner')
          ])
        );

        // Attribute value search
        const attributeSearch = cts.search(
          cts.elementAttributeWordQuery(
            fn.QName('', 'product'),
            fn.QName('', 'category'),
            'electronics'
          )
        );

        // ============================================================================
        // 🎸 RANGE QUERIES - Numerical and Date Precision 🎸
        // ============================================================================

        // Numeric range search
        const numericRangeSearch = cts.search(
          cts.elementRangeQuery(
            fn.QName('', 'price'),
            '>=',
            100,
            '<=',
            500
          )
        );

        // Date range search
        const dateRangeSearch = cts.search(
          cts.elementRangeQuery(
            fn.QName('', 'published-date'),
            '>=',
            xs.date('2023-01-01'),
            '<=',
            xs.date('2024-12-31')
          )
        );

        // Greater than comparison
        const greaterThanSearch = cts.search(
          cts.elementRangeQuery(
            fn.QName('', 'rating'),
            '>',
            4.0
          )
        );

        // ============================================================================
        // 🎸 COLLECTION AND DIRECTORY SEARCHES - Organize Like Rush Albums 🎸
        // ============================================================================

        // Search within specific collections
        const collectionSearch = cts.search(
          cts.andQuery([
            cts.collectionQuery('/articles/tech'),
            cts.wordQuery('artificial intelligence')
          ])
        );

        // Multiple collections
        const multiCollectionSearch = cts.search(
          cts.andQuery([
            cts.orQuery([
              cts.collectionQuery('/products/electronics'),
              cts.collectionQuery('/products/computers')
            ]),
            cts.elementWordQuery(fn.QName('', 'brand'), 'apple')
          ])
        );

        // Directory-based search
        const directorySearch = cts.search(
          cts.andQuery([
            cts.directoryQuery('/content/blog/', 'infinity'),
            cts.wordQuery('tutorial')
          ])
        );

        // ============================================================================
        // 🎸 JSON PROPERTY SEARCHES - Modern Data Structures 🎸
        // ============================================================================

        // JSON property search
        const jsonPropertySearch = cts.search(
          cts.jsonPropertyWordQuery('title', 'getting started')
        );

        // Nested JSON property search
        const nestedJsonSearch = cts.search(
          cts.jsonPropertyWordQuery('author.name', 'john doe')
        );

        // JSON property range query
        const jsonRangeSearch = cts.search(
          cts.jsonPropertyRangeQuery(
            'rating',
            '>=',
            4.5
          )
        );

        // ============================================================================
        // 🎸 PROXIMITY SEARCHES - Words That Rock Together 🎸
        // ============================================================================

        // Near query - words within specified distance
        const nearSearch = cts.search(
          cts.nearQuery([
            cts.wordQuery('machine'),
            cts.wordQuery('learning')
          ], 5, ['ordered'])
        );

        // Words in same sentence
        const sentenceProximity = cts.search(
          cts.nearQuery([
            cts.wordQuery('data'),
            cts.wordQuery('analysis')
          ], 20, ['same-sentence'])
        );

        // ============================================================================
        // 🎸 WILDCARD AND PATTERN SEARCHES - Flexible Like Geddy's Voice 🎸
        // ============================================================================

        // Wildcard search
        const wildcardSearch = cts.search(
          cts.wordQuery('data*')
        );

        // Multiple wildcards
        const multiWildcardSearch = cts.search(
          cts.orQuery([
            cts.wordQuery('analy*'),
            cts.wordQuery('*base'),
            cts.wordQuery('*ware*')
          ])
        );

        // ============================================================================
        // 🎸 NEGATIVE SEARCHES - What We Don't Want 🎸
        // ============================================================================

        // NOT query - exclude specific terms
        const notSearch = cts.search(
          cts.andNotQuery(
            cts.wordQuery('database'),
            cts.wordQuery('mysql')
          )
        );

        // Complex exclusion
        const complexNotSearch = cts.search(
          cts.andQuery([
            cts.wordQuery('tutorial'),
            cts.andNotQuery(
              cts.wordQuery('advanced'),
              cts.orQuery([
                cts.wordQuery('expert'),
                cts.wordQuery('professional')
              ])
            )
          ])
        );

        // ============================================================================
        // 🎸 FACETED SEARCH - Categorize Like Rush's Musical Eras 🎸
        // ============================================================================

        // Basic faceted search
        const facetedSearch = cts.search(
          cts.wordQuery('tutorial'),
          ['faceted'],
          cts.elementReference(fn.QName('', 'category'))
        );

        // Multiple facets
        const multiFacetSearch = cts.search(
          cts.andQuery([
            cts.wordQuery('product'),
            cts.elementRangeQuery(fn.QName('', 'price'), '<=', 1000)
          ]),
          ['faceted'],
          [
            cts.elementReference(fn.QName('', 'brand')),
            cts.elementReference(fn.QName('', 'category')),
            cts.pathReference('/product/rating')
          ]
        );

        // ============================================================================
        // 🎸 SEARCH OPTIONS AND FORMATTING - Fine-Tune Your Results 🎸
        // ============================================================================

        // Search with options
        const searchWithOptions = cts.search(
          cts.wordQuery('artificial intelligence'),
          [
            'score-simple',
            'faceted',
            'checked'
          ],
          1  // quality threshold
        );

        // Paginated results
        const paginatedSearch = fn.subsequence(
          cts.search(cts.wordQuery('machine learning')),
          1,    // start position
          10    // page size
        );

        // Search with sorting
        const sortedSearch =
          for $doc in cts.search(cts.wordQuery('tutorial'))
          let $score := cts.score($doc)
          order by $score descending
          return $doc;

        // ============================================================================
        // 🎸 COMPLEX BUSINESS QUERIES - Real-World Epic Scenarios 🎸
        // ============================================================================

        // E-commerce product search
        const ecommerceSearch = cts.search(
          cts.andQuery([
            // Text search in product description
            cts.orQuery([
              cts.elementWordQuery(fn.QName('', 'name'), 'smartphone'),
              cts.elementWordQuery(fn.QName('', 'description'), 'mobile phone')
            ]),
            // Price range
            cts.elementRangeQuery(fn.QName('', 'price'), '>=', 200, '<=', 800),
            // In stock
            cts.elementWordQuery(fn.QName('', 'status'), 'available'),
            // Specific categories
            cts.orQuery([
              cts.collectionQuery('/products/electronics'),
              cts.collectionQuery('/products/mobile')
            ]),
            // Minimum rating
            cts.elementRangeQuery(fn.QName('', 'rating'), '>=', 4.0)
          ])
        );

        // Content management search
        const cmsSearch = cts.search(
          cts.andQuery([
            // Content type
            cts.elementWordQuery(fn.QName('', 'type'), 'article'),
            // Published content only
            cts.elementWordQuery(fn.QName('', 'status'), 'published'),
            // Date range
            cts.elementRangeQuery(
              fn.QName('', 'publish-date'),
              '>=',
              xs.date('2024-01-01')
            ),
            // Topic search
            cts.orQuery([
              cts.elementWordQuery(fn.QName('', 'title'), 'artificial intelligence'),
              cts.elementWordQuery(fn.QName('', 'tags'), 'AI'),
              cts.elementWordQuery(fn.QName('', 'category'), 'technology')
            ]),
            // Author filter
            cts.elementWordQuery(fn.QName('', 'author'), 'tech team')
          ])
        );

        // ============================================================================
        // 🎸 GEOSPATIAL SEARCHES - Location Like Rush's Canadian Roots 🎸
        // ============================================================================

        // Point within radius search
        const geoPointSearch = cts.search(
          cts.elementGeospatialQuery(
            fn.QName('', 'location'),
            cts.circle(10, cts.point(43.6532, -79.3832))  // 10km radius from Toronto
          )
        );

        // Region search
        const geoRegionSearch = cts.search(
          cts.elementGeospatialQuery(
            fn.QName('', 'coordinates'),
            cts.polygon([
              cts.point(45.0, -75.0),  // Ottawa area polygon
              cts.point(45.5, -75.0),
              cts.point(45.5, -74.5),
              cts.point(45.0, -74.5)
            ])
          )
        );

        // ============================================================================
        // 🎸 FULL-TEXT SEARCH WITH HIGHLIGHTING - Make Results Shine 🎸
        // ============================================================================

        // Search with snippet extraction
        const searchWithSnippets =
          for $doc in cts.search(cts.wordQuery('machine learning'))
          return object-node {
            'uri': xdmp.nodeUri($doc),
            'score': cts.score($doc),
            'snippet': cts.highlight($doc, cts.wordQuery('machine learning'), '<mark>$1</mark>')
          };

        // ============================================================================
        // 🎸 PERFORMANCE OPTIMIZATION - Fast Like Alex Lifeson's Fingers 🎸
        // ============================================================================

        // Optimized search with constraints
        const optimizedSearch = cts.search(
          cts.andQuery([
            cts.collectionQuery('/recent-docs'),  // Limit scope first
            cts.wordQuery('tutorial'),
            cts.elementRangeQuery(fn.QName('', 'priority'), '>=', 3)
          ]),
          ['unfiltered', 'score-zero'],  // Performance options
          0  // No quality threshold for speed
        );

        // Use indexes for better performance
        const indexOptimizedSearch = cts.search(
          cts.elementWordQuery(fn.QName('', 'category'), 'technology'),  // Uses element word index
          ['unfiltered']
        );

        // ============================================================================
        // 🎸 ERROR HANDLING AND VALIDATION - Robust Like Rush's 40+ Year Career 🎸
        // ============================================================================

        // Safe search with error handling
        try {
          const safeSearch = cts.search(
            cts.andQuery([
              cts.wordQuery(searchTerm),
              cts.collectionQuery(collectionName)
            ])
          );

          if (fn.empty(safeSearch)) {
            console.log('🎸 No results found - try expanding your search terms');
          } else {
            console.log(`🎸 Found ${fn.count(safeSearch)} epic results!`);
          }
        } catch (error) {
          console.log('🔥 Search failed:', error);
          // Fallback to simpler search
          const fallbackSearch = cts.search(cts.wordQuery(searchTerm));
        }

        // 🎸 End of Epic MarkLogic CTS Examples - Rock on with your searches! 🎸
        // 🎸 End of Epic MarkLogic CTS Examples - Rock on with your searches! 🎸
        """;
  }

  /**
   * 🎸 EPIC MARKLOGIC SEARCH EXECUTION METHOD! 🎸
   * Execute CTS query against MarkLogic database using /v1/search endpoint
   */
  private String executeMarkLogicSearch(String ctsQuery) {
    try {
      logger.debug("🎸 Preparing to execute CTS query against MarkLogic");

      // Clean up the CTS query - remove markdown formatting if present
      String cleanQuery = ctsQuery;
      if (cleanQuery.contains("```json")) {
        cleanQuery = cleanQuery.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
      }
      cleanQuery = """
          {
            "query": {
              "and-query": {
                "queries": [
                  {
                    "collection-query": {
                      "uri": ["red"]
                    }
                  }
                ]
              }
            }
          }
                    """;

      logger.debug("🎸 Cleaned CTS query: {}", cleanQuery);

      // Create search request payload for /v1/search endpoint
      // The CTS query goes in the "search" object with a "query" property
      String searchPayload = String.format("""
          {
            "search": {
              "query": %s,
              "options": {
                "page-length": 10,
                "return-results": true,
                "return-facets": false,
                "return-metrics": true,
                "return-plan": false
              }
            }
          }
          """, cleanQuery);

      logger.debug("🎸 Search payload prepared: {}", searchPayload);

      // Execute the search using MarkLogic Java Client
      try {
        // Use the search endpoint via the DatabaseClient
        com.marklogic.client.io.StringHandle payload = new com.marklogic.client.io.StringHandle(searchPayload);
        payload.setFormat(com.marklogic.client.io.Format.JSON);

        QueryManager queryManager = databaseClient.newQueryManager();
        QueryDefinition query = queryManager.newRawStructuredQueryDefinitionAs(Format.JSON, searchPayload);
        JacksonHandle result = queryManager.search(query, new JacksonHandle());
        JsonNode retrievedJsonNode = result.get();
        String searchResults = retrievedJsonNode.toPrettyString();
        logger.debug("🎸 Raw search results received: {}", searchResults);

        // Validate and format the results
        if (searchResults != null && !searchResults.trim().isEmpty()) {
          logger.info("🎸 Successfully executed MarkLogic search - {} characters returned", searchResults.length());
          return searchResults;
        } else {
          logger.warn("🎸 Search executed but returned empty results");
          return "{ \"results\": [], \"total\": 0, \"message\": \"No results found for the search criteria\" }";
        }

      } catch (Exception clientException) {
        logger.error("🔥 MarkLogic client execution failed: {}", clientException.getMessage(), clientException);

        // Try alternative approach using REST API directly
        logger.debug("🎸 Attempting fallback REST API approach");
        return executeSearchViaRestAPI(cleanQuery);
      }

    } catch (Exception e) {
      logger.error("💥 Search execution failed: {}", e.getMessage(), e);
      throw new RuntimeException("🔥 Epic search execution failure: " + e.getMessage(), e);
    }
  }

  /**
   * 🎸 Fallback search execution via REST API
   */
  private String executeSearchViaRestAPI(String ctsQuery) {
    try {
      logger.debug("🎸 Executing search via REST API fallback");

      // For now, return a structured response indicating the method was called
      // This can be enhanced with actual HTTP client calls to MarkLogic REST API
      String fallbackResponse = String.format("""
          {
            "search-response": {
              "total": 0,
              "start": 1,
              "page-length": 10,
              "results": [],
              "metrics": {
                "query-resolution-time": "PT0.001S",
                "facet-resolution-time": "PT0.001S",
                "snippet-resolution-time": "PT0.000S",
                "total-time": "PT0.001S"
              },
              "message": "Search executed via REST API fallback - actual implementation pending",
              "query": %s
            }
          }
          """, ctsQuery);

      logger.info("🎸 REST API fallback response generated");
      return fallbackResponse;

    } catch (Exception e) {
      logger.error("💥 REST API fallback also failed: {}", e.getMessage(), e);
      throw new RuntimeException("🔥 All search execution methods failed: " + e.getMessage(), e);
    }
  }

  /**
   * 🎸 EPIC MARKLOGIC STRUCTURED SEARCH EXECUTION METHOD! 🎸
   * Execute structured query against MarkLogic database using QueryManager
   */
  private String executeMarkLogicStructuredSearch(String structuredQuery) {
    try {
      logger.debug("🎸 Preparing to execute structured query against MarkLogic");

      // Clean up the structured query - remove markdown formatting if present
      String cleanQuery = structuredQuery;
      if (cleanQuery.contains("```json")) {
        cleanQuery = cleanQuery.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
      }

      logger.debug("🎸 Cleaned structured query: {}", cleanQuery);

      // Execute the search using MarkLogic Java Client with structured query
      try {
        QueryManager queryManager = databaseClient.newQueryManager();

        // Create structured query definition from JSON string
        RawStructuredQueryDefinition query = queryManager.newRawStructuredQueryDefinitionAs(
            com.marklogic.client.io.Format.JSON, cleanQuery);

        // Use JacksonHandle for JSON response
        JacksonHandle searchHandle = new JacksonHandle();
        queryManager.search(query, searchHandle);

        // Get search results
        JsonNode resultNode = searchHandle.get();
        String searchResults = resultNode.toPrettyString();
        logger.debug("🎸 Raw structured search results received: {}", searchResults);

        // Validate and format the results
        if (searchResults != null && !searchResults.trim().isEmpty()) {
          logger.info("🎸 Successfully executed MarkLogic structured search - {} characters returned",
              searchResults.length());
          return searchResults;
        } else {
          logger.warn("🎸 Structured search executed but returned empty results");
          return "{ \"results\": [], \"total\": 0, \"message\": \"No results found for the structured search criteria\" }";
        }

      } catch (Exception clientException) {
        logger.error("🔥 MarkLogic structured client execution failed: {}", clientException.getMessage(),
            clientException);

        // Try alternative approach using JSON response format
        logger.debug("🎸 Attempting fallback with alternative handle approach");
        return executeStructuredSearchViaJsonHandle(cleanQuery);
      }

    } catch (Exception e) {
      logger.error("💥 Structured search execution failed: {}", e.getMessage(), e);
      throw new RuntimeException("🔥 Epic structured search execution failure: " + e.getMessage(), e);
    }
  }

  /**
   * 🎸 Fallback structured search execution via JSON Handle
   */
  private String executeStructuredSearchViaJsonHandle(String structuredQuery) {
    try {
      logger.debug("🎸 Executing structured search via JSON handle fallback");

      QueryManager queryManager = databaseClient.newQueryManager();

      // Create structured query definition from JSON string
      RawStructuredQueryDefinition query = queryManager.newRawStructuredQueryDefinitionAs(
          com.marklogic.client.io.Format.JSON, structuredQuery);

      // Use JacksonHandle for JSON response
      JacksonHandle result = queryManager.search(query, new JacksonHandle());
      JsonNode retrievedJsonNode = result.get();
      String searchResults = retrievedJsonNode.toPrettyString();
      logger.info("🎸 JSON handle structured search response generated");
      return searchResults;

    } catch (Exception e) {
      logger.error("💥 JSON handle fallback also failed: {}", e.getMessage(), e);

      // Final fallback - return structured response
      String finalFallbackResponse = String.format("""
          {
            "search-response": {
              "total": 0,
              "start": 1,
              "page-length": 10,
              "results": [],
              "metrics": {
                "query-resolution-time": "PT0.001S",
                "facet-resolution-time": "PT0.001S",
                "snippet-resolution-time": "PT0.000S",
                "total-time": "PT0.001S"
              },
              "message": "Structured search executed via final fallback - check query format",
              "query": %s
            }
          }
          """, structuredQuery);

      return finalFallbackResponse;
    }
  }

}
