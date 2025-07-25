package com.example.mcpserver.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

/**
 * Simple debug test to understand what's happening with the service
 */
@SpringBootTest
@ActiveProfiles("test")
class McpServiceDebugTest {

    @Autowired
    private McpService mcpService;

    @Test
    void debugServiceBehavior() {
        // Test a simple tool call
        Map<String, Object> arguments = Map.of("prompt", "test");
        Map<String, Object> result = mcpService.callTool("generate_text", arguments);

        System.out.println("🔍 Debug result: " + result);
        System.out.println("🔍 isError: " + result.get("isError"));
        System.out.println("🔍 content: " + result.get("content"));

        // Test completions
        Map<String, Object> completions = mcpService.getCompletions("test", 4);
        System.out.println("🔍 Completions: " + completions);
    }
}
