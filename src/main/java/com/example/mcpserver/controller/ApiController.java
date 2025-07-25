package com.example.mcpserver.controller;

import com.example.mcpserver.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🎸 Epic 2112-Style REST API Controller! 🎸
 * Provides REST endpoints for subscription management and resource updates
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private McpService mcpService;

    /**
     * 🚀 Epic subscription endpoint
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody Map<String, Object> request) {
        String uri = (String) request.get("uri");
        String clientId = (String) request.get("clientId");
        if (clientId == null) {
            clientId = "api-client";
        }

        logger.info("🎸 Epic API subscription request for URI: {} from client: {}", uri, clientId);
        Map<String, Object> result = mcpService.subscribeToResource(uri, clientId);
        return ResponseEntity.ok(result);
    }

    /**
     * 📋 List all epic subscriptions
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> listSubscriptions() {
        logger.info("📋 API: Listing all epic subscriptions");
        Map<String, Object> result = mcpService.listSubscriptions();
        return ResponseEntity.ok(result);
    }

    /**
     * 🔥 Simulate resource update
     */
    @PostMapping("/resource/update")
    public ResponseEntity<Map<String, Object>> simulateResourceUpdate(@RequestBody Map<String, Object> request) {
        String uri = (String) request.get("uri");
        logger.info("🔥 API: Simulating resource update for: {}", uri);
        Map<String, Object> result = mcpService.simulateResourceUpdate(uri);
        return ResponseEntity.ok(result);
    }

    /**
     * 🛑 Unsubscribe from resource
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody Map<String, Object> request) {
        String subscriptionId = (String) request.get("subscriptionId");
        logger.info("🛑 API: Unsubscription request for ID: {}", subscriptionId);
        Map<String, Object> result = mcpService.unsubscribeFromResource(subscriptionId);
        return ResponseEntity.ok(result);
    }
}
