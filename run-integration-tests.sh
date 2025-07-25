#!/bin/bash

# 🎸 Epic Integration Test Runner - 2112 Style! 🎸
# This script loads environment variables from .env and runs integration tests

set -e

echo "🎸 Loading .env file for epic integration tests..."

# Check if .env file exists
if [[ ! -f .env ]]; then
    echo "❌ .env file not found! Please create one with Azure OpenAI credentials."
    echo "Example .env content:"
    echo "AZURE_OPENAI_API_KEY=your_api_key_here"
    echo "AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/"
    echo "AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name"
    exit 1
fi

# Load .env file (ignore comments and empty lines)
export $(grep -v '^#' .env | grep -v '^$' | xargs)

echo "🔥 Running integration tests with Azure OpenAI..."
echo "   Endpoint: $AZURE_OPENAI_ENDPOINT"
echo "   Deployment: $AZURE_OPENAI_DEPLOYMENT_NAME"

# Clean and run the integration tests (always fresh execution, ignore cache)
./gradlew clean test --tests "*IntegrationTest" --rerun-tasks --info
