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
    public ResponseEntity<McpResponse> handleMcpRequest(@RequestBody McpRequest request) {
        logger.debug("handleMcpRequest");
        try {
            Object result = processRequest(request);
            return ResponseEntity.ok(new McpResponse(request.getId(), result));
        } catch (Exception e) {
            McpResponse.McpError error = new McpResponse.McpError(-32603, "Internal error", e.getMessage());
            return ResponseEntity.ok(new McpResponse(request.getId(), error));
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
                String text = (String) params.get("text");
                Integer position = (Integer) params.get("position");
                yield mcpService.getCompletions(text, position);
            }
            case "notifications/cancelled" -> {
                Object progressToken = params.get("progressToken");
                yield mcpService.cancelOperation(progressToken);
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
}
