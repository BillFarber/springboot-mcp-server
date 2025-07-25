#!/bin/bash

# 🎸 Epic MCP Server Runner - 2112 Style! 🎸
# This script runs the SpringBoot MCP Server for manual testing

set -e

echo "🎸 Starting Epic MCP Server..."

# Check if .env file exists and load it (optional for dev mode)
if [[ -f .env ]]; then
    echo "🔥 Loading .env file..."
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
    echo "   Azure OpenAI Endpoint: $AZURE_OPENAI_ENDPOINT"
    echo "   Azure OpenAI Deployment: $AZURE_OPENAI_DEPLOYMENT_NAME"
fi

echo "🚀 Launching MCP Server on http://localhost:8080"
echo "📡 MCP Endpoint: http://localhost:8080/mcp"
echo "🔗 REST API: http://localhost:8080/api/"
echo ""
echo "Press Ctrl+C to stop the server"
echo "=================================="

# Run the server
./gradlew bootRun
