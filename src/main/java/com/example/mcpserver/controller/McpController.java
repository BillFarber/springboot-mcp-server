package com.example.mcpserver.controller;

import com.example.mcpserver.model.McpRequest;
import com.example.mcpserver.model.McpResponse;
import com.example.mcpserver.service.McpService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    @Autowired
    private McpService mcpService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcpRequest(@RequestBody McpRequest request) {
        logger.debug("handleMcpRequest - DIAGNOSTIC MODE");
        logger.info("🔍 DEBUG: Received request: method={}, id={}", request.getMethod(), request.getId());

        try {
            Object result = processRequest(request);
            logger.info("🔍 DEBUG: Process result type: {}",
                    result != null ? result.getClass().getSimpleName() : "null");
            logger.info("🔍 DEBUG: Process result: {}", result);

            // Return raw Map for diagnosis instead of McpResponse
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", request.getId());
            response.put("result", result != null ? result : Map.of("debug", "null result"));
            response.put("error", null);

            // Log the complete response structure
            logger.info("🔍 DEBUG: Complete response structure:");
            logger.info("🔍 DEBUG: - jsonrpc: {}", response.get("jsonrpc"));
            logger.info("🔍 DEBUG: - id: {}", response.get("id"));
            logger.info("🔍 DEBUG: - result: {}", response.get("result"));
            logger.info("🔍 DEBUG: - error: {}", response.get("error"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("🔍 DEBUG: Exception occurred", e);
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", request.getId());
            errorResponse.put("result", null);
            errorResponse.put("error", Map.of(
                    "code", -32603,
                    "message", "Internal error",
                    "data", e.getMessage() != null ? e.getMessage() : "Unknown error"));
            return ResponseEntity.ok(errorResponse);
        }
    }

    private Object processRequest(McpRequest request) {
        String method = request.getMethod();
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getParams();

        return switch (method) {
            case "initialize" -> {
                // Extract client protocol version if provided
                String clientProtocolVersion = null;
                if (params != null && params.containsKey("protocolVersion")) {
                    clientProtocolVersion = (String) params.get("protocolVersion");
                }
                yield mcpService.getServerInfo(clientProtocolVersion);
            }
            case "tools/list" -> Map.of("tools", mcpService.listTools());
            case "resources/list" -> Map.of("resources", mcpService.listResources());
            case "resources/templates/list" -> Map.of("resourceTemplates", mcpService.listResourceTemplates());
            case "prompts/list" -> Map.of("prompts", mcpService.listPrompts());
            case "prompts/get" -> {
                String name = (String) params.get("name");
                yield mcpService.getPrompt(name);
            }
            case "completion/complete" -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> argument = (Map<String, Object>) params.get("argument");
                String text = argument != null ? (String) argument.get("value") : null;
                Integer position = text != null ? text.length() : 0;
                yield mcpService.getCompletions(text, position);
            }
            case "notifications/cancelled" -> {
                Object progressToken = params.get("progressToken");
                yield mcpService.cancelOperation(progressToken);
            }
            case "notifications/initialized" -> {
                // This is a notification that doesn't require a response, but we need to handle
                // it
                logger.info("🔍 DEBUG: Client initialized notification received");
                yield Map.of("success", true);
            }
            case "ping" -> Map.of("result", "pong");
            case "tools/call" -> {
                String toolName = (String) params.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
                yield mcpService.callTool(toolName, arguments);
            }
            case "resources/read" -> {
                String uri = (String) params.get("uri");
                yield mcpService.readResource(uri);
            }
            case "resources/subscribe" -> {
                String uri = (String) params.get("uri");
                String clientId = params.containsKey("clientId") ? (String) params.get("clientId") : "http-client";
                yield mcpService.subscribeToResource(uri, clientId);
            }
            case "resources/unsubscribe" -> {
                String subscriptionId = (String) params.get("subscriptionId");
                yield mcpService.unsubscribeFromResource(subscriptionId);
            }
            case "resources/list_subscriptions" -> mcpService.listSubscriptions();
            case "resources/simulate_update" -> {
                String uri = (String) params.get("uri");
                yield mcpService.simulateResourceUpdate(uri);
            }
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "server", "SpringBoot MCP Server",
                "version", "1.0.0"));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<Object> capabilities() {
        return ResponseEntity.ok(mcpService.getServerInfo());
    }

    // 🎸 Epic 2112-Style Subscription Endpoints! 🎸

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestParam String uri,
            @RequestParam(defaultValue = "http-client") String clientId) {
        logger.info("🎸 Epic subscription request for URI: {} from client: {}", uri, clientId);
        Map<String, Object> result = mcpService.subscribeToResource(uri, clientId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/subscribe/{subscriptionId}")
    public ResponseEntity<Map<String, Object>> unsubscribe(@PathVariable String subscriptionId) {
        logger.info("🛑 Unsubscription request for ID: {}", subscriptionId);
        Map<String, Object> result = mcpService.unsubscribeFromResource(subscriptionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> listSubscriptions() {
        logger.info("📋 Listing all epic subscriptions");
        Map<String, Object> result = mcpService.listSubscriptions();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<Map<String, Object>> getSubscription(@PathVariable String subscriptionId) {
        logger.info("🔍 Getting subscription details for: {}", subscriptionId);
        Map<String, Object> result = mcpService.getSubscriptionDetails(subscriptionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/simulate-update")
    public ResponseEntity<Map<String, Object>> simulateUpdate(@RequestParam String uri) {
        logger.info("🔥 Simulating resource update for: {}", uri);
        Map<String, Object> result = mcpService.simulateResourceUpdate(uri);
        return ResponseEntity.ok(result);
    }
}
