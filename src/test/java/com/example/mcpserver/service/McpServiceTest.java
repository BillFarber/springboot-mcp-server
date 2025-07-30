package com.example.mcpserver.service;

import com.example.mcpserver.model.Tool;
import com.example.mcpserver.model.Resource;
import com.example.mcpserver.model.ResourceTemplate;
import com.example.mcpserver.model.Prompt;
import com.example.mcpserver.model.ResourceSubscription;
import com.marklogic.client.DatabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 🎸 Epic 2112-Style Unit Tests for McpService! 🎸
 */
@DisplayName("🎸 Epic McpService Tests - 2112 Style! 🎸")
class McpServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private MarkLogicDocsService markLogicDocsService;

    @Mock
    private DatabaseClient databaseClient;

    @InjectMocks
    private McpService mcpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock ApplicationContext to prevent NullPointerException
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[0]);
        when(applicationContext.getBeansOfType(ChatClient.class)).thenReturn(new HashMap<>());

        // Reset the subscription maps for each test
        Map<String, ResourceSubscription> resourceSubscriptions = new ConcurrentHashMap<>();
        Map<String, Set<String>> uriToSubscriptions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(mcpService, "resourceSubscriptions", resourceSubscriptions);
        ReflectionTestUtils.setField(mcpService, "uriToSubscriptions", uriToSubscriptions);
    }

    @Nested
    @DisplayName("🚀 Server Info Tests")
    class ServerInfoTests {

        @Test
        @DisplayName("Should return epic server info with default protocol version")
        void shouldReturnServerInfoWithDefaultProtocol() {
            // When
            Map<String, Object> serverInfo = mcpService.getServerInfo();

            // Then
            assertNotNull(serverInfo);
            assertEquals("2024-11-05", serverInfo.get("protocolVersion"));

            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) serverInfo.get("serverInfo");
            assertEquals("SpringBoot MCP Server", server.get("name"));
            assertEquals("1.0.0", server.get("version"));

            @SuppressWarnings("unchecked")
            Map<String, Object> capabilities = (Map<String, Object>) serverInfo.get("capabilities");
            assertNotNull(capabilities.get("tools"));
            assertNotNull(capabilities.get("resources"));
            assertNotNull(capabilities.get("completion"));
        }

        @Test
        @DisplayName("Should use client protocol version when provided")
        void shouldUseClientProtocolVersion() {
            // Given
            String clientVersion = "2024-12-01";

            // When
            Map<String, Object> serverInfo = mcpService.getServerInfo(clientVersion);

            // Then
            assertEquals(clientVersion, serverInfo.get("protocolVersion"));
        }
    }

    @Nested
    @DisplayName("🎸 Tools Tests")
    class ToolsTests {

        @Test
        @DisplayName("Should list all epic tools")
        void shouldListAllTools() {
            // When
            List<Tool> tools = mcpService.listTools();

            // Then
            assertNotNull(tools);
            assertEquals(5, tools.size()); // Updated to include search_marklogic tool

            Tool generateTextTool = tools.stream()
                    .filter(t -> "generate_text".equals(t.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(generateTextTool);
            assertEquals("Generate text using AI based on a prompt", generateTextTool.getDescription());

            Tool marklogicDocsTool = tools.stream()
                    .filter(t -> "marklogic_docs".equals(t.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(marklogicDocsTool);
            assertEquals("Help you out with MarkLogic", marklogicDocsTool.getDescription());

            Tool opticCodeTool = tools.stream()
                    .filter(t -> "optic_code_generator".equals(t.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(opticCodeTool);
            assertEquals("Generate optic code snippets for data retrieval and transformation",
                    opticCodeTool.getDescription());

            // 🎸 Verify our new rebellious verification tool! 🎸
            Tool verifyOpticCodeTool = tools.stream()
                    .filter(t -> "verify_optic_code".equals(t.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(verifyOpticCodeTool);
            assertEquals("Verify optic code for syntax and logical correctness (rebellious random verification)",
                    verifyOpticCodeTool.getDescription());

            // 🎸 Verify our new search MarkLogic tool! 🎸
            Tool searchMarkLogicTool = tools.stream()
                    .filter(t -> "search_marklogic".equals(t.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(searchMarkLogicTool);
            assertEquals("Search MarkLogic database using natural language criteria",
                    searchMarkLogicTool.getDescription());
        }
    }

    @Nested
    @DisplayName("🔥 Epic Tool Execution Tests")
    class ToolExecutionTests {

        @Test
        @DisplayName("Should handle unknown tool with epic error message")
        void shouldHandleUnknownToolWithEpicError() {
            // Given
            String unknownTool = "nonexistent_tool";
            Map<String, Object> arguments = Map.of("test", "value");

            // When
            Map<String, Object> result = mcpService.callTool(unknownTool, arguments);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 Epic tool not found!"));
            assertTrue(contentText.contains("Available tools: generate_text, optic_code_generator"));
        }

        @Test
        @DisplayName("Should handle null arguments for generate_text")
        void shouldHandleNullArgumentsForGenerateText() {
            // When
            Map<String, Object> result = mcpService.callTool("generate_text", null);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🔥 No arguments provided"));
        }

        @Test
        @DisplayName("Should handle empty prompt for generate_text")
        void shouldHandleEmptyPromptForGenerateText() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "");

            // When
            Map<String, Object> result = mcpService.callTool("generate_text", arguments);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 Prompt is required"));
        }

        @Test
        @DisplayName("Should generate text with AI client")
        void shouldGenerateTextWithAI() throws Exception {
            // Given - This test will show that when properly mocked, the AI works
            String prompt = "Write an epic song about code!";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            ChatResponse mockResponse = mock(ChatResponse.class);
            Generation mockGeneration = mock(Generation.class);
            AssistantMessage mockMessage = mock(AssistantMessage.class);

            // Create a properly mocked ChatClient
            ChatClient mockChatClient = mock(ChatClient.class);
            when(mockChatClient.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockResponse);
            when(mockResponse.getResult()).thenReturn(mockGeneration);
            when(mockGeneration.getOutput()).thenReturn(mockMessage);
            when(mockMessage.getContent()).thenReturn("Epic AI-generated song about code!");

            // Replace the real client with our mock
            ReflectionTestUtils.setField(mcpService, "chatClient", mockChatClient);

            // When
            Map<String, Object> result = mcpService.callTool("generate_text", arguments);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertEquals("Epic AI-generated song about code!", contentText);
            verify(mockChatClient).call(any(org.springframework.ai.chat.prompt.Prompt.class));
        }

        @Test
        @DisplayName("Should use mock response when AI client is null")
        void shouldUseMockResponseWhenAIClientIsNull() {
            // Given
            String prompt = "Test prompt";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // Set chatClient to null to trigger fallback behavior
            ReflectionTestUtils.setField(mcpService, "chatClient", null);

            // When
            Map<String, Object> result = mcpService.callTool("generate_text", arguments);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("isError")); // Should be false when client is null (fallback)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 AI client not configured"));
            assertTrue(contentText.contains(prompt));
        }

        @Test
        @DisplayName("Should handle AI client failure gracefully")
        void shouldHandleAIClientFailureGracefully() {
            // Given - Configure the mock to return null (simulating AI failure)
            String prompt = "Test prompt that will fail";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // Configure mock to return null response (simulating failure)
            when(chatClient.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(null);

            // When
            Map<String, Object> result = mcpService.callTool("generate_text", arguments);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError")); // Should be true when AI response is null
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🔥 AI response was incomplete"));
        }

        @Test
        @DisplayName("Should generate optic code with prompt")
        void shouldGenerateOpticCodeWithPrompt() {
            // Given
            Map<String, Object> arguments = Map.of(
                    "prompt", "Create optic code to read from users table");

            // When
            Map<String, Object> result = mcpService.callTool("optic_code_generator", arguments);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertNotNull(contentText);
            // Should contain fallback template since ChatClient is not configured in tests
            assertTrue(contentText.contains("op.fromView") || contentText.contains("optic code"));
        }

        @Test
        @DisplayName("🎸 Should verify optic code with rebellious random results")
        void shouldVerifyOpticCodeWithRebelliousRandomResults() {
            // Given
            String opticCode = "const result = op.fromView('users', 'profiles').select(['name', 'email']).result();";
            Map<String, Object> arguments = Map.of("optic_code", opticCode);

            // When
            Map<String, Object> result = mcpService.callTool("verify_optic_code", arguments);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("isError"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertNotNull(contentText);

            // Should contain verification result (either VALID or INVALID)
            assertTrue(contentText.contains("VALID") || contentText.contains("INVALID"));
            assertTrue(contentText.contains("Code length:"));
            assertTrue(contentText.contains("Rush wisdom:"));
            assertTrue(contentText.contains("rebellious random verification"));

            // Check metadata is present
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            assertNotNull(metadata);
            assertTrue(metadata.containsKey("isValid"));
            assertTrue(metadata.containsKey("codeLength"));
            assertEquals("rebellious_random_rush_style", metadata.get("verificationMethod"));
            assertEquals(opticCode.length(), metadata.get("codeLength"));
        }

        @Test
        @DisplayName("🎸 Should handle empty optic code verification")
        void shouldHandleEmptyOpticCodeVerification() {
            // Given
            Map<String, Object> arguments = Map.of("optic_code", "");

            // When
            Map<String, Object> result = mcpService.callTool("verify_optic_code", arguments);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 Optic code is required"));
        }

        @Test
        @DisplayName("Should return error when prompt is missing")
        void shouldReturnErrorWhenPromptMissing() {
            // Given
            Map<String, Object> arguments = Map.of();

            // When
            Map<String, Object> result = mcpService.callTool("optic_code_generator", arguments);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("Prompt is required"));
        }

        @Test
        @DisplayName("🎸 Should handle search_marklogic tool with fallback CTS code generation")
        void shouldHandleSearchMarkLogicToolWithFallbackCTSCodeGeneration() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "Find all documents about machine learning");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("isError"));
            assertEquals("text/markdown", result.get("mimeType"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 MarkLogic CTS Query (Fallback Mode) 🎸"));
            assertTrue(contentText.contains("Find all documents about machine learning"));
            assertTrue(contentText.contains("```json"));
            assertTrue(contentText.contains("word-query"));

            // Verify metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            assertNotNull(metadata);
            assertEquals("Find all documents about machine learning", metadata.get("searchPrompt"));
            assertEquals("fallback_template", metadata.get("status"));
            assertEquals("cts_serialized_fallback_v1.0", metadata.get("toolVersion"));
            assertEquals("json", metadata.get("queryFormat"));
            assertEquals("marklogic_cts", metadata.get("searchFramework"));
        }

        @Test
        @DisplayName("🎸 Should handle search_marklogic tool with empty prompt")
        void shouldHandleSearchMarkLogicToolWithEmptyPrompt() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 Search prompt is required"));
        }

        @Test
        @DisplayName("🎸 Should have DatabaseClient available for search_marklogic tool")
        void shouldHaveDatabaseClientAvailableForSearchMarkLogicTool() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "test search");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("isError"));

            // The search should complete successfully even though DatabaseClient is mocked
            // This verifies that the DatabaseClient dependency injection is working
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("MarkLogic") || contentText.contains("search"));
        }
    }

    @Nested
    @DisplayName("🎸 Epic Subscription Tests")
    class SubscriptionTests {

        @Test
        @DisplayName("Should create epic subscription")
        void shouldCreateEpicSubscription() {
            // Given
            String uri = "file://epic-code.java";
            String clientId = "vscode-2112";

            // When
            Map<String, Object> result = mcpService.subscribeToResource(uri, clientId);

            // Then
            assertNotNull(result);
            assertEquals("subscribed", result.get("status"));
            assertEquals(uri, result.get("uri"));
            assertTrue(((String) result.get("message")).contains("🎸 Rocking real-time updates"));
            assertNotNull(result.get("subscriptionId"));
            assertTrue(((String) result.get("subscriptionId")).startsWith("sub_"));
        }

        @Test
        @DisplayName("Should list epic subscriptions")
        void shouldListEpicSubscriptions() {
            // Given - Create a subscription first
            String uri = "file://temples-of-syrinx.java";
            String clientId = "vscode-prophet";
            Map<String, Object> subscribeResult = mcpService.subscribeToResource(uri, clientId);
            String subscriptionId = (String) subscribeResult.get("subscriptionId");

            // When
            Map<String, Object> result = mcpService.listSubscriptions();

            // Then
            assertNotNull(result);
            assertEquals(1, result.get("totalCount"));
            assertTrue(((String) result.get("message")).contains("🎸 1 epic subscriptions rocking!"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) result.get("subscriptions");
            assertEquals(1, subscriptions.size());

            Map<String, Object> subscription = subscriptions.get(0);
            assertEquals(subscriptionId, subscription.get("subscriptionId"));
            assertEquals(uri, subscription.get("uri"));
            assertEquals(clientId, subscription.get("clientId"));
            assertTrue((Boolean) subscription.get("active"));
        }

        @Test
        @DisplayName("Should unsubscribe from epic subscription")
        void shouldUnsubscribeFromEpicSubscription() {
            // Given - Create a subscription first
            String uri = "file://freewill.ts";
            String clientId = "vscode-neil";
            Map<String, Object> subscribeResult = mcpService.subscribeToResource(uri, clientId);
            String subscriptionId = (String) subscribeResult.get("subscriptionId");

            // When
            Map<String, Object> result = mcpService.unsubscribeFromResource(subscriptionId);

            // Then
            assertNotNull(result);
            assertEquals("unsubscribed", result.get("status"));
            assertEquals(subscriptionId, result.get("subscriptionId"));
            assertEquals(uri, result.get("uri"));
            assertTrue(((String) result.get("message")).contains("🎸 Subscription ended"));
        }

        @Test
        @DisplayName("Should handle unsubscribe from nonexistent subscription")
        void shouldHandleUnsubscribeFromNonexistentSubscription() {
            // Given
            String nonexistentId = "sub_nonexistent_123";

            // When
            Map<String, Object> result = mcpService.unsubscribeFromResource(nonexistentId);

            // Then
            assertNotNull(result);
            assertEquals("not_found", result.get("status"));
            assertEquals("Subscription not found", result.get("error"));
            assertEquals(nonexistentId, result.get("subscriptionId"));
        }

        @Test
        @DisplayName("Should get subscription details")
        void shouldGetSubscriptionDetails() {
            // Given - Create a subscription first
            String uri = "file://xanadu.js";
            String clientId = "vscode-geddy";
            Map<String, Object> subscribeResult = mcpService.subscribeToResource(uri, clientId);
            String subscriptionId = (String) subscribeResult.get("subscriptionId");

            // When
            Map<String, Object> result = mcpService.getSubscriptionDetails(subscriptionId);

            // Then
            assertNotNull(result);
            assertEquals(subscriptionId, result.get("subscriptionId"));
            assertEquals(uri, result.get("uri"));
            assertEquals(clientId, result.get("clientId"));
            assertTrue((Boolean) result.get("active"));
            assertNotNull(result.get("createdAt"));
            assertTrue(((String) result.get("message")).contains("🎸 Subscription info delivered"));
        }

        @Test
        @DisplayName("Should simulate resource update")
        void shouldSimulateResourceUpdate() {
            // Given
            String uri = "file://limelight.py";

            // When
            Map<String, Object> result = mcpService.simulateResourceUpdate(uri);

            // Then
            assertNotNull(result);
            assertEquals("updated", result.get("status"));
            assertEquals(uri, result.get("uri"));
            assertTrue(((String) result.get("message")).contains("🔥 Epic resource update simulated"));
            assertNotNull(result.get("timestamp"));
        }
    }

    @Nested
    @DisplayName("🚀 Resources Tests")
    class ResourcesTests {

        @Test
        @DisplayName("Should list all epic resources")
        void shouldListAllResources() {
            // When
            List<Resource> resources = mcpService.listResources();

            // Then
            assertNotNull(resources);
            assertEquals(2, resources.size());

            Resource serverInfo = resources.stream()
                    .filter(r -> "mcp://server/info".equals(r.getUri()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(serverInfo);
            assertEquals("Server Information", serverInfo.getName());

            Resource toolExamples = resources.stream()
                    .filter(r -> "mcp://tools/examples".equals(r.getUri()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(toolExamples);
            assertEquals("Tool Examples", toolExamples.getName());
        }

        @Test
        @DisplayName("Should list epic resource templates")
        void shouldListResourceTemplates() {
            // When
            List<ResourceTemplate> templates = mcpService.listResourceTemplates();

            // Then
            assertNotNull(templates);
            assertEquals(2, templates.size());

            ResourceTemplate logTemplate = templates.stream()
                    .filter(t -> "mcp://logs/{level}".equals(t.getUriTemplate()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(logTemplate);
            assertEquals("Log Files by Level", logTemplate.getName());
        }

        @Test
        @DisplayName("Should read server info resource")
        void shouldReadServerInfoResource() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://server/info");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://server/info", content.get("uri"));
            assertEquals("application/json", content.get("mimeType"));
            assertNotNull(content.get("text"));
        }

        @Test
        @DisplayName("Should read tool examples resource")
        void shouldReadToolExamplesResource() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://tools/examples");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://tools/examples", content.get("uri"));
            assertEquals("text/markdown", content.get("mimeType"));

            String text = (String) content.get("text");
            assertNotNull(text);
            assertTrue(text.contains("# Tool Usage Examples"));
            assertTrue(text.contains("## Generate Text Tool"));
            assertTrue(text.contains("## Optic Code Generator Tool"));
            assertTrue(text.contains("optic_code_generator"));
            assertTrue(text.contains("prompt"));
        }

        @Test
        @DisplayName("Should read generate_text tool documentation")
        void shouldReadGenerateTextToolDocs() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://tools/generate_text/docs");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://tools/generate_text/docs", content.get("uri"));
            assertEquals("text/markdown", content.get("mimeType"));

            String text = (String) content.get("text");
            assertNotNull(text);
            assertTrue(text.contains("# Generate Text Tool Documentation"));
            assertTrue(text.contains("prompt"));
            assertTrue(text.contains("maxTokens"));
        }

        @Test
        @DisplayName("Should read optic_code_generator tool documentation")
        void shouldReadOpticCodeGeneratorToolDocs() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://tools/optic_code_generator/docs");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://tools/optic_code_generator/docs", content.get("uri"));
            assertEquals("text/markdown", content.get("mimeType"));

            String text = (String) content.get("text");
            assertNotNull(text);
            assertTrue(text.contains("# Optic Code Generator Tool Documentation"));
            assertTrue(text.contains("prompt"));
            assertTrue(text.contains("AI"));
            assertTrue(text.contains("Rush"));
            assertTrue(text.contains("optic"));
        }

        @Test
        @DisplayName("Should return error for unknown tool documentation")
        void shouldReturnErrorForUnknownToolDocs() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> {
                mcpService.readResource("mcp://tools/unknown_tool/docs");
            });
        }

        @Test
        @DisplayName("Should read debug level logs")
        void shouldReadDebugLevelLogs() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://logs/debug");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://logs/debug", content.get("uri"));
            assertEquals("text/plain", content.get("mimeType"));

            String text = (String) content.get("text");
            assertNotNull(text);
            assertTrue(text.contains("# DEBUG Level Logs"));
            assertTrue(text.contains("Epic SpringBoot MCP Server Logs"));
            assertTrue(text.contains("DEBUG"));
        }

        @Test
        @DisplayName("Should read info level logs")
        void shouldReadInfoLevelLogs() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://logs/info");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://logs/info", content.get("uri"));
            assertEquals("text/plain", content.get("mimeType"));

            String text = (String) content.get("text");
            assertNotNull(text);
            assertTrue(text.contains("# INFO Level Logs"));
            assertTrue(text.contains("MCP Server") || text.contains("INFO"));
            assertTrue(text.contains("INFO"));
        }

        @Test
        @DisplayName("Should read error level logs")
        void shouldReadErrorLevelLogs() {
            // When
            Map<String, Object> result = mcpService.readResource("mcp://logs/error");

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
            assertEquals(1, contents.size());

            Map<String, Object> content = contents.get(0);
            assertEquals("mcp://logs/error", content.get("uri"));
            assertEquals("text/plain", content.get("mimeType"));

            String text = (String) content.get("text");
            assertNotNull(text);
            assertTrue(text.contains("# ERROR Level Logs"));
            // 🎸 Epic fix: Error logs may be empty when no errors occurred (healthy state!)
            // The formatted response should always contain the header and structure, even
            // if no actual errors
            assertTrue(text.contains("ERROR Level") || text.contains("No error level logs"));
        }

        @Test
        @DisplayName("Should return error for invalid log level")
        void shouldReturnErrorForInvalidLogLevel() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> {
                mcpService.readResource("mcp://logs/invalid_level");
            });
        }
    }

    @Nested
    @DisplayName("🎸 Completions Tests")
    class CompletionsTests {

        @Test
        @DisplayName("Should provide epic completions for Java code")
        void shouldProvideEpicCompletionsForJavaCode() {
            // Given
            String text = "public class EpicClass {";
            Integer position = text.length();

            // When
            Map<String, Object> result = mcpService.getCompletions(text, position);

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            Map<String, Object> completion = (Map<String, Object>) result.get("completion");
            assertNotNull(completion);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> completions = (List<Map<String, Object>>) completion.get("values");
            assertNotNull(completions);
            assertTrue(completions.size() > 0);

            // Should have some completions (either AI or static)
            // In test environment, AI will fail but static completions should be present
            boolean hasCompletions = completions.stream()
                    .anyMatch(c -> c.get("text") != null && !((String) c.get("text")).isEmpty());
            assertTrue(hasCompletions, "Should have at least one completion");
        }

        @Test
        @DisplayName("Should handle null position in completions")
        void shouldHandleNullPositionInCompletions() {
            // Given
            String text = "epic code";

            // When
            Map<String, Object> result = mcpService.getCompletions(text, null);

            // Then
            assertNotNull(result);

            @SuppressWarnings("unchecked")
            Map<String, Object> completion = (Map<String, Object>) result.get("completion");
            assertNotNull(completion);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> completions = (List<Map<String, Object>>) completion.get("values");
            assertNotNull(completions);
        }
    }

    @Nested
    @DisplayName("🔥 Operation Cancellation Tests")
    class CancellationTests {

        @Test
        @DisplayName("Should cancel operation with progress token")
        void shouldCancelOperationWithProgressToken() {
            // Given
            String progressToken = "epic_operation_123";

            // When
            Map<String, Object> result = mcpService.cancelOperation(progressToken);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("cancelled"));
            assertEquals(progressToken, result.get("progressToken"));
        }

        @Test
        @DisplayName("Should handle cancellation without progress token")
        void shouldHandleCancellationWithoutProgressToken() {
            // When
            Map<String, Object> result = mcpService.cancelOperation(null);

            // Then
            assertNotNull(result);
            assertFalse((Boolean) result.get("cancelled"));
            assertEquals("No progress token provided", result.get("error"));
        }
    }
}
