package com.example.mcpserver.controller;

import com.example.mcpserver.model.McpRequest;
import com.example.mcpserver.model.McpResponse;
import com.example.mcpserver.service.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
public class McpController {

    @Autowired
    private McpService mcpService;

    @PostMapping
    public ResponseEntity<McpResponse> handleMcpRequest(@RequestBody McpRequest request) {
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
            case "initialize" -> mcpService.getServerInfo();
            case "tools/list" -> Map.of("tools", mcpService.listTools());
            case "resources/list" -> Map.of("resources", mcpService.listResources());
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
