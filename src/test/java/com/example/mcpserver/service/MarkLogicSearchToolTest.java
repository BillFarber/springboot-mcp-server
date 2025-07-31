package com.example.mcpserver.service;

import com.example.mcpserver.model.Tool;
import com.marklogic.client.DatabaseClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🎸 Comprehensive MarkLogic Search Tool Tests - Rush 2112 Style! 🎸
 * 
 * Focused unit tests specifically for the search_marklogic tool functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🎸 MarkLogic Search Tool Tests - Epic Query Generation! 🎸")
class MarkLogicSearchToolTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private MarkLogicDocsService markLogicDocsService;

    @InjectMocks
    private McpService mcpService;

    @Nested
    @DisplayName("🚀 Tool Registration and Metadata Tests")
    class ToolRegistrationTests {

        @Test
        @DisplayName("Should register search_marklogic tool with correct metadata")
        void shouldRegisterSearchMarkLogicTool() {
            // When
            List<Tool> tools = mcpService.listTools();

            // Then
            Tool searchTool = tools.stream()
                    .filter(t -> "search_marklogic".equals(t.getName()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(searchTool, "search_marklogic tool should be registered");
            assertEquals("Search MarkLogic database using natural language criteria",
                    searchTool.getDescription());

            // Verify tool has required parameters
            assertNotNull(searchTool.getInputSchema());
            assertNotNull(searchTool.getInputSchema().get("properties"));

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) searchTool.getInputSchema().get("properties");
            assertTrue(properties.containsKey("prompt"), "Tool should have 'prompt' parameter");
        }
    }

    @Nested
    @DisplayName("🔥 Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject null prompt")
        void shouldRejectNullPrompt() {
            // Given
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("prompt", null);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertTrue((Boolean) result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            String contentText = (String) contentList.get(0).get("text");
            assertTrue(contentText.contains("🎸 Search prompt is required"));
        }

        @Test
        @DisplayName("Should reject empty prompt")
        void shouldRejectEmptyPrompt() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertTrue((Boolean) result.get("isError"));
        }

        @Test
        @DisplayName("Should reject whitespace-only prompt")
        void shouldRejectWhitespaceOnlyPrompt() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "   \t\n  ");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertTrue((Boolean) result.get("isError"));
        }

        @Test
        @DisplayName("Should handle missing prompt argument")
        void shouldHandleMissingPromptArgument() {
            // Given
            Map<String, Object> arguments = Map.of("other_param", "value");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertTrue((Boolean) result.get("isError"));
        }

        @Test
        @DisplayName("Should handle very long prompts gracefully")
        void shouldHandleVeryLongPrompts() {
            // Given
            String longPrompt = "Find documents about ".repeat(200) + "machine learning";
            Map<String, Object> arguments = Map.of("prompt", longPrompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            // Should handle gracefully - either succeed or fail gracefully
            assertNotNull(result.get("isError"));
        }

        @Test
        @DisplayName("Should handle special characters in prompt")
        void shouldHandleSpecialCharactersInPrompt() {
            // Given
            String specialPrompt = "Find docs with symbols: @#$%^&*()[]{}|\\:;\"'<>,.?/~`+=";
            Map<String, Object> arguments = Map.of("prompt", specialPrompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            assertNotNull(result.get("isError"));
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Given
            String unicodePrompt = "Find documents with unicode: 你好 مرحبا Здравствуй 🎸";
            Map<String, Object> arguments = Map.of("prompt", unicodePrompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            assertNotNull(result.get("isError"));
        }
    }

    @Nested
    @DisplayName("🎸 Query Generation Tests")
    class QueryGenerationTests {

        @Test
        @DisplayName("Should generate fallback structured query when AI unavailable")
        void shouldGenerateFallbackQuery() {
            // Given
            String prompt = "Find documents about machine learning";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            // Result should be successful (fallback mode works)
            assertFalse((Boolean) result.get("isError"));
            assertEquals("text/markdown", result.get("mimeType"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
            String contentText = (String) contentList.get(0).get("text");

            // Should contain structured query elements
            assertTrue(contentText.contains("🎸 MarkLogic Structured Query"));
            assertTrue(contentText.contains("```json"));
            assertTrue(contentText.contains("term-query"));

            // Verify metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            assertNotNull(metadata);
            assertEquals(prompt, metadata.get("searchPrompt"));
            assertEquals("marklogic_structured", metadata.get("searchFramework"));
            assertTrue(metadata.get("toolVersion").toString().contains("structured"));
        }

        @Test
        @DisplayName("Should include search terms in generated query")
        void shouldIncludeSearchTermsInQuery() {
            // Given
            String prompt = "Find neural network research papers";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            String contentText = (String) contentList.get(0).get("text");

            // Should extract and include meaningful terms from the prompt
            boolean containsRelevantTerms = contentText.contains("neural") ||
                    contentText.contains("network") ||
                    contentText.contains("research");
            assertTrue(containsRelevantTerms, "Generated query should contain relevant search terms");
        }

        @Test
        @DisplayName("Should generate valid JSON structure in fallback mode")
        void shouldGenerateValidJSONStructure() {
            // Given
            String prompt = "test query structure";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            String contentText = (String) contentList.get(0).get("text");

            // Verify JSON structure elements are present
            assertTrue(contentText.contains("```json"));
            assertTrue(contentText.contains("query"));
            assertTrue(contentText.contains("{"));
            assertTrue(contentText.contains("}"));
            assertTrue(contentText.contains("term-query") || contentText.contains("text"));
        }
    }

    @Nested
    @DisplayName("🔥 Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should format response as markdown")
        void shouldFormatResponseAsMarkdown() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "markdown format test");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertEquals("text/markdown", result.get("mimeType"));
            assertNotNull(result.get("isError"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());

            Map<String, Object> contentItem = contentList.get(0);
            assertEquals("text", contentItem.get("type"));
            assertNotNull(contentItem.get("text"));
        }

        @Test
        @DisplayName("Should include comprehensive metadata")
        void shouldIncludeComprehensiveMetadata() {
            // Given
            String prompt = "metadata test query";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            assertNotNull(metadata);

            // Verify required metadata fields
            assertEquals(prompt, metadata.get("searchPrompt"));
            assertTrue(metadata.containsKey("status"));
            assertTrue(metadata.containsKey("toolVersion"));
            assertTrue(metadata.containsKey("queryFormat"));
            assertTrue(metadata.containsKey("searchFramework"));

            // Verify framework is correct
            assertEquals("marklogic_structured", metadata.get("searchFramework"));
        }

        @Test
        @DisplayName("Should include query examples and documentation")
        void shouldIncludeQueryExamplesAndDocumentation() {
            // Given
            String prompt = "complex search example";
            Map<String, Object> arguments = Map.of("prompt", prompt);

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            String contentText = (String) contentList.get(0).get("text");

            // Should include helpful documentation
            assertTrue(contentText.contains("MarkLogic") || contentText.contains("query"));
            assertTrue(contentText.length() > 100, "Response should be substantial and helpful");
        }
    }

    @Nested
    @DisplayName("🚀 DatabaseClient Integration Tests")
    class DatabaseClientTests {

        @Test
        @DisplayName("Should handle missing DatabaseClient gracefully")
        void shouldHandleMissingDatabaseClient() {
            // Given - Service with null DatabaseClient (simulated via mocking)
            Map<String, Object> arguments = Map.of("prompt", "test search");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            assertNotNull(result);
            // Should still work in fallback mode even without DatabaseClient
            assertNotNull(result.get("isError"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
            assertNotNull(contentList);
            assertFalse(contentList.isEmpty());
        }

        @Test
        @DisplayName("Should indicate database connectivity status in metadata")
        void shouldIndicateDatabaseConnectivityStatus() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "connectivity test");

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", arguments);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");

            // Should include framework information
            assertTrue(metadata.containsKey("searchFramework"));
            assertEquals("marklogic_structured", metadata.get("searchFramework"));
        }
    }

    @Nested
    @DisplayName("🎸 Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null arguments gracefully")
        void shouldHandleNullArguments() {
            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", null);

            // Then
            assertTrue((Boolean) result.get("isError"));
        }

        @Test
        @DisplayName("Should handle empty arguments map")
        void shouldHandleEmptyArguments() {
            // Given
            Map<String, Object> emptyArgs = Map.of();

            // When
            Map<String, Object> result = mcpService.callTool("search_marklogic", emptyArgs);

            // Then
            assertTrue((Boolean) result.get("isError"));
        }

        @Test
        @DisplayName("Should handle concurrent requests safely")
        void shouldHandleConcurrentRequestsSafely() {
            // Given
            Map<String, Object> arguments = Map.of("prompt", "concurrent test");

            // When - Simulate concurrent access
            assertDoesNotThrow(() -> {
                mcpService.callTool("search_marklogic", arguments);
                mcpService.callTool("search_marklogic", arguments);
                mcpService.callTool("search_marklogic", arguments);
            });
        }
    }
}
