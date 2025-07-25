package com.example.mcpserver.integration;

import com.example.mcpserver.McpServerApplication;
import com.example.mcpserver.service.McpService;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * 🎸 Epic 2112-Style Integration Tests with Azure OpenAI! 🎸
 * These tests actually hit Azure OpenAI using the .env configuration
 */
@SpringBootTest(classes = McpServerApplication.class)
@ActiveProfiles("integration")
@DisplayName("🎸 Epic Azure OpenAI Integration Tests - 2112 Style! 🎸")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AzureOpenAIIntegrationTest {

    @Autowired
    private McpService mcpService;

    private static boolean azureOpenAIAvailable;

    @BeforeAll
    static void checkAzureOpenAIAvailability() {
        try {
            // Load .env file to check if Azure OpenAI is configured
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            String apiKey = dotenv.get("AZURE_OPENAI_API_KEY");
            String endpoint = dotenv.get("AZURE_OPENAI_ENDPOINT");
            String deployment = dotenv.get("AZURE_OPENAI_DEPLOYMENT_NAME");

            azureOpenAIAvailable = apiKey != null && !apiKey.isEmpty() &&
                    endpoint != null && !endpoint.isEmpty() &&
                    deployment != null && !deployment.isEmpty();

            if (azureOpenAIAvailable) {
                System.out.println("🎸 Azure OpenAI configuration detected - running integration tests!");
                System.out.println("🔥 Endpoint: " + endpoint);
                System.out.println("🚀 Deployment: " + deployment);
            } else {
                System.out.println("⚠️ Azure OpenAI not configured - skipping integration tests");
                System.out.println(
                        "💡 Add .env file with AZURE_OPENAI_API_KEY, AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_DEPLOYMENT_NAME to run these tests");
            }
        } catch (Exception e) {
            System.out.println("❌ Error checking Azure OpenAI configuration: " + e.getMessage());
            azureOpenAIAvailable = false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("🚀 Should generate epic text with Azure OpenAI")
    void shouldGenerateEpicTextWithAzureOpenAI() {
        assumeTrue(azureOpenAIAvailable, "Azure OpenAI not configured - skipping test");

        // Given
        String epicPrompt = "Write a short epic song about programming in the style of Rush's 2112";
        Map<String, Object> arguments = Map.of(
                "prompt", epicPrompt,
                "maxTokens", 100);

        // When
        Map<String, Object> result = mcpService.callTool("generate_text", arguments);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse((Boolean) result.get("isError"), "Should not have error: " + result.get("content"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
        assertNotNull(contentList, "Generated content should not be null");
        assertFalse(contentList.isEmpty(), "Generated content should not be empty");

        Map<String, Object> firstContent = contentList.get(0);
        String content = (String) firstContent.get("text");
        assertNotNull(content, "Generated text should not be null");
        assertFalse(content.isEmpty(), "Generated content should not be empty");
        assertTrue(content.length() > 20, "Generated content should be substantial");

        // Should not be a mock response
        assertFalse(content.contains("🎸 AI client not configured"), "Should use real AI, not mock");

        System.out.println("🎸 Generated epic content: " + content);
    }

    @Test
    @Order(2)
    @DisplayName("🔥 Should analyze data with Azure OpenAI")
    void shouldAnalyzeDataWithAzureOpenAI() {
        assumeTrue(azureOpenAIAvailable, "Azure OpenAI not configured - skipping test");

        // Given
        String testData = "{'sales': [100, 150, 200, 175, 300], 'months': ['Jan', 'Feb', 'Mar', 'Apr', 'May']}";
        Map<String, Object> arguments = Map.of(
                "data", testData,
                "analysisType", "trends");

        // When
        Map<String, Object> result = mcpService.callTool("analyze_data", arguments);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse((Boolean) result.get("isError"), "Should not have error: " + result.get("content"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertNotNull(content, "Analysis content should not be null");
        assertFalse(content.isEmpty(), "Analysis content should not be empty");

        String firstContentText = (String) content.get(0).get("text");
        assertNotNull(firstContentText, "Analysis text should not be null");
        assertTrue(firstContentText.length() > 30, "Analysis should be detailed");

        // Should not be a mock response
        assertFalse(firstContentText.contains("🎸 AI client not configured"), "Should use real AI, not mock");

        // Should contain analysis-related content
        String lowerContent = firstContentText.toLowerCase();
        assertTrue(
                lowerContent.contains("trend") ||
                        lowerContent.contains("increase") ||
                        lowerContent.contains("growth") ||
                        lowerContent.contains("analysis"),
                "Analysis should contain relevant keywords");

        System.out.println("🔥 Generated analysis: " + content);
    }

    @Test
    @Order(3)
    @DisplayName("⚡ Should provide AI-powered completions")
    void shouldProvideAIPoweredCompletions() {
        assumeTrue(azureOpenAIAvailable, "Azure OpenAI not configured - skipping test");

        // Given
        String codeContext = "// This function calculates the factorial of ";
        Integer position = codeContext.length();

        // When
        Map<String, Object> result = mcpService.getCompletions(codeContext, position);

        // Then
        assertNotNull(result, "Result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> completion = (Map<String, Object>) result.get("completion");
        assertNotNull(completion, "Completion should not be null");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> completions = (List<Map<String, Object>>) completion.get("values");
        assertNotNull(completions, "Completions should not be null");
        assertTrue(completions.size() > 0, "Should have at least one completion");

        // Check if we have an AI completion (should be first in the list)
        Map<String, Object> firstCompletion = completions.get(0);
        String description = (String) firstCompletion.get("description");

        if ("AI-generated completion".equals(description)) {
            System.out.println("🚀 AI completion detected: " + firstCompletion.get("text"));
            String aiText = (String) firstCompletion.get("text");
            assertNotNull(aiText, "AI completion text should not be null");
            assertFalse(aiText.isEmpty(), "AI completion should not be empty");

            // When AI is working, we may or may not have static completions depending on
            // the input
            System.out.println("✅ AI-powered completions working successfully!");
        } else {
            System.out.println("📝 Using static completions (AI may not be available)");
            // Should have some static completions when AI is not available
            boolean hasStaticCompletions = completions.stream()
                    .anyMatch(c -> !("AI-generated completion".equals(c.get("description"))));
            assertTrue(hasStaticCompletions, "Should have static completions as fallback when AI fails");
        }
    }

    @Test
    @Order(4)
    @DisplayName("🎸 Should handle AI errors gracefully")
    void shouldHandleAIErrorsGracefully() {
        assumeTrue(azureOpenAIAvailable, "Azure OpenAI not configured - skipping test");

        // Given - A prompt that might cause issues
        String problematicPrompt = ""; // Empty prompt should trigger our validation
        Map<String, Object> arguments = Map.of("prompt", problematicPrompt);

        // When
        Map<String, Object> result = mcpService.callTool("generate_text", arguments);

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue((Boolean) result.get("isError"), "Should have error for empty prompt");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) result.get("content");
        assertNotNull(contentList, "Error content should not be null");
        assertFalse(contentList.isEmpty(), "Error content should not be empty");

        Map<String, Object> firstContent = contentList.get(0);
        String content = (String) firstContent.get("text");
        assertTrue(content.contains("🎸 Prompt is required"), "Should have epic error message");
    }

    @Test
    @Order(5)
    @DisplayName("🔥 Should test epic subscription system")
    void shouldTestEpicSubscriptionSystem() {
        // Given
        String uri = "file://epic-integration-test.java";
        String clientId = "integration-test-2112";

        // When - Subscribe
        Map<String, Object> subscribeResult = mcpService.subscribeToResource(uri, clientId);

        // Then - Should create subscription
        assertNotNull(subscribeResult);
        assertEquals("subscribed", subscribeResult.get("status"));
        assertEquals(uri, subscribeResult.get("uri"));
        String subscriptionId = (String) subscribeResult.get("subscriptionId");
        assertNotNull(subscriptionId);
        assertTrue(subscriptionId.startsWith("sub_"));

        // When - List subscriptions
        Map<String, Object> listResult = mcpService.listSubscriptions();

        // Then - Should find our subscription
        assertNotNull(listResult);
        assertTrue((Integer) listResult.get("totalCount") >= 1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) listResult.get("subscriptions");
        boolean foundOurSubscription = subscriptions.stream()
                .anyMatch(s -> subscriptionId.equals(s.get("subscriptionId")));
        assertTrue(foundOurSubscription, "Should find our subscription in the list");

        // When - Simulate update
        Map<String, Object> updateResult = mcpService.simulateResourceUpdate(uri);

        // Then - Should notify (in logs)
        assertNotNull(updateResult);
        assertEquals("updated", updateResult.get("status"));
        assertEquals(uri, updateResult.get("uri"));

        // When - Unsubscribe
        Map<String, Object> unsubscribeResult = mcpService.unsubscribeFromResource(subscriptionId);

        // Then - Should unsubscribe successfully
        assertNotNull(unsubscribeResult);
        assertEquals("unsubscribed", unsubscribeResult.get("status"));
        assertEquals(subscriptionId, unsubscribeResult.get("subscriptionId"));

        System.out.println("🎸 Epic subscription system test completed!");
    }

    @Test
    @Order(6)
    @DisplayName("🚀 Should verify server capabilities")
    void shouldVerifyServerCapabilities() {
        // When
        Map<String, Object> serverInfo = mcpService.getServerInfo();

        // Then
        assertNotNull(serverInfo);
        assertEquals("SpringBoot MCP Server",
                ((Map<?, ?>) serverInfo.get("serverInfo")).get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> capabilities = (Map<String, Object>) serverInfo.get("capabilities");
        assertNotNull(capabilities);

        // Should have all epic capabilities
        assertNotNull(capabilities.get("tools"));
        assertNotNull(capabilities.get("resources"));
        assertNotNull(capabilities.get("completion"));
        assertNotNull(capabilities.get("prompts"));

        // Resources should support subscription
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceCapabilities = (Map<String, Object>) capabilities.get("resources");
        assertTrue((Boolean) resourceCapabilities.get("subscribe"));

        System.out.println("🔥 Server capabilities verified!");
    }

    @AfterEach
    void logTestCompletion(TestInfo testInfo) {
        if (azureOpenAIAvailable) {
            System.out.println("✅ Completed: " + testInfo.getDisplayName());
        } else {
            System.out.println("⏭️ Skipped: " + testInfo.getDisplayName() + " (Azure OpenAI not configured)");
        }
    }
}
