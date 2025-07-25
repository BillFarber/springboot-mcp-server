<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# MCP Server Project Instructions

This is a Model Context Protocol (MCP) server built with SpringBoot and SpringAI using Gradle as the build tool.

## Key Technologies
- **SpringBoot 3.2.0**: Main application framework
- **SpringAI**: For AI integration and chat capabilities  
- **Gradle**: Build tool and dependency management
- **Java 17**: Programming language
- **Jackson**: JSON serialization/deserialization

## Project Structure
- `src/main/java/com/example/mcpserver/model/`: MCP protocol models (McpRequest, McpResponse, Tool, Resource)
- `src/main/java/com/example/mcpserver/service/`: Business logic for MCP operations
- `src/main/java/com/example/mcpserver/controller/`: REST API endpoints for MCP communication
- `src/main/resources/`: Configuration files

## MCP Protocol Implementation
This server implements the Model Context Protocol specification, providing:
- Tool listing and execution capabilities
- Resource management and reading
- AI-powered text generation and data analysis tools
- JSON-RPC 2.0 communication protocol

## AI Integration
The server uses SpringAI for:
- Text generation based on prompts
- Data analysis and insights
- Integration with OpenAI GPT models (configurable)

## Development Guidelines
- Follow MCP specification for all protocol interactions
- Use SpringBoot best practices for service architecture
- Implement proper error handling with MCP error codes
- Add comprehensive logging for debugging
- Write unit tests for all service methods

You can find more info and examples at https://modelcontextprotocol.io/llms-full.txt
