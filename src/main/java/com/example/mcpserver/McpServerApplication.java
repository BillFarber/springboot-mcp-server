package com.example.mcpserver;

import com.example.mcpserver.model.McpRequest;
import com.example.mcpserver.model.McpResponse;
import com.example.mcpserver.service.McpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        // Check if stdio mode is requested
        boolean stdioMode = args.length > 0 && "stdio".equals(args[0]);

        if (stdioMode) {
            // For stdio mode, redirect all Spring Boot logs to stderr
            System.setProperty("logging.level.root", "WARN");
            System.setProperty("logging.pattern.console", "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            System.setProperty("spring.main.banner-mode", "off");
            System.setProperty("spring.output.ansi.enabled", "never");

            // Start Spring context without web server for stdio mode
            System.setProperty("spring.main.web-application-type", "none");
            ApplicationContext context = SpringApplication.run(McpServerApplication.class, args);
            McpService mcpService = context.getBean(McpService.class);
            startStdioLoop(mcpService);
        } else {
            // Start normal HTTP server
            SpringApplication.run(McpServerApplication.class, args);
        }
    }

    private static void startStdioLoop(McpService mcpService) {
        ObjectMapper objectMapper = new ObjectMapper();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            System.err.println("MCP stdio server started, waiting for requests...");

            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("Received: " + line);
                try {
                    // Parse incoming JSON-RPC request
                    McpRequest request = objectMapper.readValue(line, McpRequest.class);
                    System.err.println("Parsed request method: " + request.getMethod());

                    // Check if this is a notification (no response expected)
                    boolean isNotification = request.getId() == null ||
                            request.getMethod().startsWith("notifications/") ||
                            request.getMethod().equals("initialized");

                    if (isNotification) {
                        System.err.println("Handling notification: " + request.getMethod());
                        // Process notification but don't send response
                        processRequest(request, mcpService);
                        System.err.println("Notification processed");
                    } else {
                        // Process the request and send response
                        Object result = processRequest(request, mcpService);
                        McpResponse response = new McpResponse(request.getId(), result);

                        // Send response directly to stdout
                        String responseJson = objectMapper.writeValueAsString(response);
                        System.err.println("Sending response: " + responseJson);

                        // Write directly to stdout and flush immediately
                        System.out.println(responseJson);
                        System.out.flush();

                        // Double-check that it was written
                        System.err.println("Response sent to stdout");
                    }

                } catch (Exception e) {
                    System.err.println("Error processing request: " + e.getMessage());
                    e.printStackTrace();
                    // Send error response
                    try {
                        McpResponse.McpError error = new McpResponse.McpError(-32603, "Internal error", e.getMessage());
                        McpResponse response = new McpResponse("unknown", error);
                        String responseJson = objectMapper.writeValueAsString(response);
                        System.err.println("Sending error response: " + responseJson);

                        // Write error response directly to stdout
                        System.out.println(responseJson);
                        System.out.flush();
                    } catch (Exception ex) {
                        System.err.println("Failed to send error response: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in stdio loop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Object processRequest(McpRequest request, McpService mcpService) {
        String method = request.getMethod();
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getParams();

        return switch (method) {
            case "initialize" -> {
                System.err.println("Processing initialize request with params: " + params);
                // Extract client protocol version if provided
                String clientProtocolVersion = null;
                if (params != null && params.containsKey("protocolVersion")) {
                    clientProtocolVersion = (String) params.get("protocolVersion");
                    System.err.println("Client protocol version: " + clientProtocolVersion);
                }
                yield mcpService.getServerInfo(clientProtocolVersion);
            }
            case "initialized" -> {
                System.err.println("Received initialized notification");
                yield Map.of(); // Empty response for notification
            }
            case "notifications/initialized" -> {
                System.err.println("Received notifications/initialized notification - connection established");
                yield Map.of(); // Empty response for notification
            }
            case "tools/list" -> Map.of("tools", mcpService.listTools());
            case "resources/list" -> Map.of("resources", mcpService.listResources());
            case "resources/templates/list" -> Map.of("resourceTemplates", mcpService.listResourceTemplates());
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
            default -> {
                System.err.println("Unknown method: " + method);
                throw new IllegalArgumentException("Unknown method: " + method);
            }
        };
    }
}