# SpringBoot MCP Server

A Model Context Protocol (MCP) server built with SpringBoot and SpringAI, providing AI-powered tools and resources through a standardized protocol interface.

## Features

- **MCP Protocol Compliance**: Full implementation of the Model Context Protocol specification
- **AI-Powered Tools**: Text generation and data analysis using SpringAI
- **RESTful API**: HTTP-based MCP communication
- **Extensible Architecture**: Easy to add new tools and resources
- **Production Ready**: Built with SpringBoot best practices

## Available Tools

### 1. Text Generation (`generate_text`)
Generate text content using AI based on prompts.

**Parameters:**
- `prompt` (string, required): The text prompt to generate content from
- `maxTokens` (integer, optional): Maximum number of tokens to generate (default: 100)

### 2. Data Analysis (`analyze_data`)
Analyze data and provide insights using AI.

**Parameters:**
- `data` (string, required): Data to analyze (JSON string or CSV)
- `analysisType` (string, required): Type of analysis (`summary`, `trends`, `insights`)

## Available Resources

- `mcp://server/info`: Server information and capabilities
- `mcp://tools/examples`: Examples of how to use the available tools

## Prerequisites

- Java 17 or higher
- Gradle 8.5 or higher

## Getting Started

### 1. Clone and Build

```bash
./gradlew build
```

### 2. Configure AI (Optional)

To enable AI features, you have several options for configuring your Azure OpenAI credentials:

#### Option A: Using .env file (Recommended for development)

Create a `.env` file in the project root directory:

```env
# Azure OpenAI Configuration - Keep this file secret!
AZURE_OPENAI_API_KEY=your-azure-openai-api-key
AZURE_OPENAI_ENDPOINT=https://your-resource-name.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT_NAME=gpt-35-turbo
```

**Important:** The `.env` file is automatically ignored by git and won't be committed to version control.

#### Option B: Using environment variables

```bash
export AZURE_OPENAI_API_KEY=your-azure-openai-api-key
export AZURE_OPENAI_ENDPOINT=https://your-resource-name.openai.azure.com/
export AZURE_OPENAI_DEPLOYMENT_NAME=gpt-35-turbo
```

#### Option C: Direct configuration (Not recommended for production)

Set your credentials directly in `src/main/resources/application.properties`:

```properties
spring.ai.azure.openai.api-key=your-azure-openai-api-key
spring.ai.azure.openai.endpoint=https://your-resource-name.openai.azure.com/
spring.ai.azure.openai.chat.options.deployment-name=gpt-35-turbo
spring.ai.azure.openai.chat.options.model=gpt-3.5-turbo
```

### 3. Run the Server

```bash
./gradlew bootRun
```

The server will start on `http://localhost:8080`

### 4. Test the Server

Check server health:
```bash
curl -s http://localhost:8080/mcp/health | jq .
```

Get server capabilities:
```bash
curl -s http://localhost:8080/mcp/capabilities | jq .
```

## Current Status

✅ **MCP Server Working**: The server implements the full MCP protocol and responds to all tool calls
✅ **Mock AI Responses**: Tools provide mock responses when AI is not configured
⚠️ **Azure OpenAI Integration**: Currently providing mock responses (SpringAI auto-configuration needs debugging)

## Testing

The server is fully functional for MCP protocol testing. All tools work with mock responses. MCP Tool Calls

### List Available Tools
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }' | jq .
```

### Test Text Generation Tool
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "tools/call",
    "params": {
      "name": "generate_text",
      "arguments": {
        "prompt": "Write a haiku about coding",
        "maxTokens": 50
      }
    }
  }' | jq .
```

### Test Data Analysis Tool
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "3",
    "method": "tools/call",
    "params": {
      "name": "analyze_data",
      "arguments": {
        "data": "{\"sales\": [100, 150, 200, 175], \"months\": [\"Jan\", \"Feb\", \"Mar\", \"Apr\"]}",
        "analysisType": "trends"
      }
    }
  }' | jq .
```

### List Available Resources
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "4",
    "method": "resources/list",
    "params": {}
  }' | jq .
```

### Read a Resource
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "5",
    "method": "resources/read",
    "params": {
      "uri": "mcp://tools/examples"
    }
  }' | jq .
```

### Initialize MCP Connection
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "0",
    "method": "initialize",
    "params": {}
  }' | jq .
```

## MCP Client Integration

### Initialize Connection
```json
POST /mcp
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {}
}
```

### List Available Tools
```json
POST /mcp
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/list",
  "params": {}
}
```

### Call a Tool
```json
POST /mcp
{
  "jsonrpc": "2.0",
  "id": "3",
  "method": "tools/call",
  "params": {
    "name": "generate_text",
    "arguments": {
      "prompt": "Write a haiku about programming",
      "maxTokens": 50
    }
  }
}
```

### List Resources
```json
POST /mcp
{
  "jsonrpc": "2.0",
  "id": "4",
  "method": "resources/list",
  "params": {}
}
```

### Read a Resource
```json
POST /mcp
{
  "jsonrpc": "2.0",
  "id": "5",
  "method": "resources/read",
  "params": {
    "uri": "mcp://server/info"
  }
}
```

## Development

### Adding New Tools

1. Update the `listTools()` method in `McpService` to include your new tool
2. Add a case for your tool in the `callTool()` method
3. Implement the tool logic as a private method

### Adding New Resources

1. Update the `listResources()` method in `McpService`
2. Add a case for your resource in the `readResource()` method
3. Implement the resource reading logic

### Running Tests

```bash
./gradlew test
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/mcpserver/
│   │   ├── controller/          # REST API controllers
│   │   ├── model/              # MCP protocol models
│   │   ├── service/            # Business logic
│   │   └── McpServerApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/example/mcpserver/
        └── McpServerApplicationTests.java
```

## Configuration

Key configuration options in `application.properties`:

- `server.port`: Server port (default: 8080)
- `spring.ai.azure.openai.api-key`: Azure OpenAI API key for AI features
- `spring.ai.azure.openai.endpoint`: Azure OpenAI service endpoint
- `spring.ai.azure.openai.chat.options.deployment-name`: Your deployment name
- `spring.ai.azure.openai.chat.options.model`: AI model to use
- Logging levels for debugging

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Learn More

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [SpringBoot Documentation](https://spring.io/projects/spring-boot)
- [SpringAI Documentation](https://spring.io/projects/spring-ai)
