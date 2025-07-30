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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🎸 Epic HTTP Controller Integration Tests - 2112 Style! 🎸
 * Tests the REST API endpoints including subscription management
 */
@SpringBootTest(classes = McpServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("🎸 Epic HTTP Controller Integration Tests! 🎸")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private String testSubscriptionId;

        @Test
        @Order(1)
        @DisplayName("🚀 Should handle MCP initialization request")
        void shouldHandleMcpInitializationRequest() throws Exception {
                // Given
                Map<String, Object> mcpRequest = Map.of(
                                "jsonrpc", "2.0",
                                "id", 1,
                                "method", "initialize",
                                "params", Map.of(
                                                "protocolVersion", "2024-11-05",
                                                "capabilities", Map.of(
                                                                "roots", Map.of("listChanged", true)),
                                                "clientInfo", Map.of(
                                                                "name", "epic-test-client",
                                                                "version", "2112.0")));

                // When & Then
                mockMvc.perform(post("/mcp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mcpRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.result.serverInfo.name").value("SpringBoot MCP Server"))
                                .andExpect(jsonPath("$.result.capabilities.tools").exists())
                                .andExpect(jsonPath("$.result.capabilities.resources").exists())
                                .andExpect(jsonPath("$.result.capabilities.resources.subscribe").value(true));
        }

        @Test
        @Order(2)
        @DisplayName("🔥 Should list available tools")
        void shouldListAvailableTools() throws Exception {
                // Given
                Map<String, Object> mcpRequest = Map.of(
                                "jsonrpc", "2.0",
                                "id", 2,
                                "method", "tools/list");

                // When & Then
                mockMvc.perform(post("/mcp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mcpRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.result.tools").isArray())
                                .andExpect(jsonPath("$.result.tools[?(@.name == 'generate_text')]").exists());
        }

        @Test
        @Order(3)
        @DisplayName("⚡ Should execute generate_text tool")
        void shouldExecuteGenerateTextTool() throws Exception {
                // Given
                Map<String, Object> mcpRequest = Map.of(
                                "jsonrpc", "2.0",
                                "id", 3,
                                "method", "tools/call",
                                "params", Map.of(
                                                "name", "generate_text",
                                                "arguments", Map.of(
                                                                "prompt", "Write a short epic poem about programming",
                                                                "maxTokens", 50)));

                // When & Then
                mockMvc.perform(post("/mcp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mcpRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                                .andExpect(jsonPath("$.id").value(3))
                                .andExpect(jsonPath("$.result.content").isArray())
                                .andExpect(jsonPath("$.result.content[0].type").value("text"))
                                .andExpect(jsonPath("$.result.content[0].text").isString());
        }

        @Test
        @Order(4)
        @DisplayName("🎸 Should handle unknown tool gracefully")
        void shouldHandleUnknownToolGracefully() throws Exception {
                // Given
                Map<String, Object> mcpRequest = Map.of(
                                "jsonrpc", "2.0",
                                "id", 4,
                                "method", "tools/call",
                                "params", Map.of(
                                                "name", "unknown_epic_tool",
                                                "arguments", Map.of()));

                // When & Then
                mockMvc.perform(post("/mcp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mcpRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                                .andExpect(jsonPath("$.id").value(4))
                                .andExpect(jsonPath("$.result.isError").value(true))
                                .andExpect(jsonPath("$.result.content[0].text")
                                                .value(containsString("🎸 Epic tool not found!")));
        }

        @Test
        @Order(5)
        @DisplayName("🚀 Should create subscription via REST API")
        void shouldCreateSubscriptionViaRestAPI() throws Exception {
                // Given
                Map<String, Object> subscriptionRequest = Map.of(
                                "uri", "file://epic-integration-test.java",
                                "clientId", "rest-test-2112");

                // When & Then
                String response = mockMvc.perform(post("/api/subscribe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(subscriptionRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value("subscribed"))
                                .andExpect(jsonPath("$.uri").value("file://epic-integration-test.java"))
                                .andExpect(jsonPath("$.subscriptionId").isString())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                // Store subscription ID for later tests
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                testSubscriptionId = (String) responseMap.get("subscriptionId");
                assertNotNull(testSubscriptionId, "Should have subscription ID");
        }

        @Test
        @Order(6)
        @DisplayName("🔥 Should list subscriptions via REST API")
        void shouldListSubscriptionsViaRestAPI() throws Exception {
                mockMvc.perform(get("/api/subscriptions"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.totalCount").isNumber())
                                .andExpect(jsonPath("$.subscriptions").isArray());
        }

        @Test
        @Order(7)
        @DisplayName("⚡ Should simulate resource update via REST API")
        void shouldSimulateResourceUpdateViaRestAPI() throws Exception {
                // Given
                Map<String, Object> updateRequest = Map.of(
                                "uri", "file://epic-integration-test.java");

                // When & Then
                mockMvc.perform(post("/api/resource/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value("updated"))
                                .andExpect(jsonPath("$.uri").value("file://epic-integration-test.java"));
        }

        @Test
        @Order(8)
        @DisplayName("🎸 Should unsubscribe via REST API")
        void shouldUnsubscribeViaRestAPI() throws Exception {
                assumeTrue(testSubscriptionId != null, "Need subscription ID from previous test");

                // Given
                Map<String, Object> unsubscribeRequest = Map.of(
                                "subscriptionId", testSubscriptionId);

                // When & Then
                mockMvc.perform(post("/api/unsubscribe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(unsubscribeRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value("unsubscribed"))
                                .andExpect(jsonPath("$.subscriptionId").value(testSubscriptionId));
        }

        @Test
        @Order(9)
        @DisplayName("🚀 Should provide completions via MCP")
        void shouldProvideCompletionsViaMCP() throws Exception {
                // Given
                Map<String, Object> mcpRequest = Map.of(
                                "jsonrpc", "2.0",
                                "id", 9,
                                "method", "completion/complete",
                                "params", Map.of(
                                                "ref", Map.of(
                                                                "type", "ref",
                                                                "name", "test-completion"),
                                                "argument", Map.of(
                                                                "name", "text",
                                                                "value",
                                                                "// This function calculates the factorial of ")));

                // When & Then
                mockMvc.perform(post("/mcp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mcpRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                                .andExpect(jsonPath("$.id").value(9))
                                .andExpect(jsonPath("$.result.completion.values").isArray());
        }

        @Test
        @Order(10)
        @DisplayName("🔥 Should handle malformed JSON gracefully")
        void shouldHandleMalformedJSONGracefully() throws Exception {
                mockMvc.perform(post("/mcp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json"))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @AfterEach
        void logTestCompletion(TestInfo testInfo) {
                System.out.println("✅ Completed: " + testInfo.getDisplayName());
        }

        private void assumeTrue(boolean condition, String message) {
                if (!condition) {
                        Assumptions.assumeTrue(false, message);
                }
        }
}
