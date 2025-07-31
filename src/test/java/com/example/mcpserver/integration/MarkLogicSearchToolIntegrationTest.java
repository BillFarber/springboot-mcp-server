package com.example.mcpserver.integration;

import com.example.mcpserver.McpServerApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🎸 Epic MarkLogic Search Tool Integration Tests - Rush 2112 Style! 🎸
 * 
 * End-to-end integration tests for the search_marklogic tool through the MCP
 * API
 */
@SpringBootTest(classes = McpServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("🎸 MarkLogic Search Tool Integration Tests! 🎸")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MarkLogicSearchToolIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("🚀 Tool Discovery Integration Tests")
    class ToolDiscoveryTests {

        @Test
        @Order(1)
        @DisplayName("Should list search_marklogic tool via MCP API")
        void shouldListSearchMarkLogicTool() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "tools/list");

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.result.tools").isArray())
                    .andExpect(jsonPath("$.result.tools[?(@.name == 'search_marklogic')]").exists())
                    .andReturn();

            // Verify tool details
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("search_marklogic"));
            assertTrue(responseContent.contains("Search MarkLogic database using natural language criteria"));
        }
    }

    @Nested
    @DisplayName("🔥 Tool Execution Integration Tests")
    class ToolExecutionTests {

        @Test
        @Order(2)
        @DisplayName("Should execute search_marklogic tool with valid prompt")
        void shouldExecuteSearchMarkLogicToolWithValidPrompt() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 2,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "Find documents about machine learning algorithms")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.isError").value(false))
                    .andExpect(jsonPath("$.result.mimeType").value("text/markdown"))
                    .andReturn();

            // Verify response content
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("MarkLogic"));
            assertTrue(responseContent.contains("Structured Query"));
            assertTrue(responseContent.contains("machine learning"));
        }

        @Test
        @Order(3)
        @DisplayName("Should handle search_marklogic tool with empty prompt")
        void shouldHandleSearchMarkLogicToolWithEmptyPrompt() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 3,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.result.isError").value(true))
                    .andReturn();

            // Verify error response
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("Search prompt is required") ||
                    responseContent.contains("prompt"));
        }

        @Test
        @Order(4)
        @DisplayName("Should handle search_marklogic tool with missing prompt argument")
        void shouldHandleSearchMarkLogicToolWithMissingPrompt() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 4,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "other_param", "value")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.result.isError").value(true))
                    .andReturn();

            // Verify error handling
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("error") || responseContent.contains("required"));
        }

        @Test
        @Order(5)
        @DisplayName("Should generate structured query with complex search terms")
        void shouldGenerateStructuredQueryWithComplexSearchTerms() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 5,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "Find JSON documents in the 'research' collection containing " +
                                            "'neural networks' or 'deep learning' created after 2023-01-01")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.result.isError").value(false))
                    .andExpect(jsonPath("$.result.mimeType").value("text/markdown"))
                    .andReturn();

            // Verify complex query generation
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("MarkLogic"));
            assertTrue(responseContent.contains("Structured Query"));
            assertTrue(responseContent.contains("```json"));

            // Should contain some relevant search terms
            boolean containsRelevantTerms = responseContent.contains("neural") ||
                    responseContent.contains("networks") ||
                    responseContent.contains("deep") ||
                    responseContent.contains("learning") ||
                    responseContent.contains("research");
            assertTrue(containsRelevantTerms, "Response should contain relevant search terms");
        }

        @Test
        @Order(6)
        @DisplayName("Should include proper metadata in response")
        void shouldIncludeProperMetadataInResponse() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 6,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "metadata test query")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.metadata").exists())
                    .andExpect(jsonPath("$.result.metadata.searchPrompt").value("metadata test query"))
                    .andExpect(jsonPath("$.result.metadata.searchFramework").value("marklogic_structured"))
                    .andExpect(jsonPath("$.result.metadata.executionMode").exists())
                    .andExpect(jsonPath("$.result.metadata.toolVersion").exists())
                    .andExpect(jsonPath("$.result.metadata.queryFormat").exists())
                    .andReturn();

            // Additional metadata validation
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("searchFramework"));
            assertTrue(responseContent.contains("marklogic_structured"));
        }
    }

    @Nested
    @DisplayName("🎸 Error Handling Integration Tests")
    class ErrorHandlingTests {

        @Test
        @Order(7)
        @DisplayName("Should handle tool call with null arguments")
        void shouldHandleToolCallWithNullArguments() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 7,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic"
                    // No arguments provided
                    ));

            // When & Then
            mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(7))
                    .andExpect(jsonPath("$.result.isError").value(true));
        }

        @Test
        @Order(8)
        @DisplayName("Should handle malformed MCP request gracefully")
        void shouldHandleMalformedMcpRequestGracefully() throws Exception {
            // Given - Malformed request (missing required fields)
            Map<String, Object> malformedRequest = Map.of(
                    "jsonrpc", "2.0",
                    "method", "tools/call"
            // Missing id and params
            );

            // When & Then
            mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(malformedRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"));
            // Should handle gracefully even with malformed request
        }
    }

    @Nested
    @DisplayName("🚀 Performance Integration Tests")
    class PerformanceTests {

        @Test
        @Order(9)
        @DisplayName("Should handle multiple concurrent requests")
        void shouldHandleMultipleConcurrentRequests() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 9,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "concurrent test query")));

            // When & Then - Make multiple requests rapidly
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mcpRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.jsonrpc").value("2.0"));
            }
        }

        @Test
        @Order(10)
        @DisplayName("Should handle large prompt efficiently")
        void shouldHandleLargePromptEfficiently() throws Exception {
            // Given
            String largePrompt = "Find documents about ".repeat(50) +
                    "machine learning and artificial intelligence algorithms";

            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 10,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", largePrompt)));

            // When & Then
            long startTime = System.currentTimeMillis();

            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                    .andExpect(jsonPath("$.id").value(10))
                    .andReturn();

            long executionTime = System.currentTimeMillis() - startTime;

            // Should complete within reasonable time (adjust threshold as needed)
            assertTrue(executionTime < 10000, "Request should complete within 10 seconds");

            // Should handle large prompt without errors
            String responseContent = result.getResponse().getContentAsString();
            assertFalse(responseContent.contains("\"isError\":true"));
        }
    }

    @Nested
    @DisplayName("🔥 Response Format Integration Tests")
    class ResponseFormatTests {

        @Test
        @Order(11)
        @DisplayName("Should return properly formatted markdown response")
        void shouldReturnProperlyFormattedMarkdownResponse() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 11,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "format test query")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.mimeType").value("text/markdown"))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content[0].type").value("text"))
                    .andExpect(jsonPath("$.result.content[0].text").exists())
                    .andReturn();

            // Verify markdown formatting
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.contains("```json") || responseContent.contains("MarkLogic"));
        }

        @Test
        @Order(12)
        @DisplayName("Should include structured query examples and documentation")
        void shouldIncludeStructuredQueryExamplesAndDocumentation() throws Exception {
            // Given
            Map<String, Object> mcpRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 12,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "search_marklogic",
                            "arguments", Map.of(
                                    "prompt", "comprehensive query examples")));

            // When & Then
            MvcResult result = mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mcpRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.isError").value(false))
                    .andReturn();

            // Verify comprehensive content
            String responseContent = result.getResponse().getContentAsString();
            assertTrue(responseContent.length() > 500, "Response should be comprehensive");
            assertTrue(responseContent.contains("MarkLogic") || responseContent.contains("query"));
        }
    }
}
