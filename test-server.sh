#!/bin/bash

# 🎸 Epic MCP Server Test Script - 2112 Style! 🎸
# This script tests the MCP server endpoints

set -e

SERVER_URL="http://localhost:8080"

echo "🎸 Testing Epic MCP Server..."
echo "==============================="

# Test 1: Health Check
echo ""
echo "1️⃣ Health Check:"
curl -s "$SERVER_URL/mcp/health" | jq '.' || echo "Failed to get health status"

# Test 2: Server Capabilities
echo ""
echo "2️⃣ Server Capabilities:"
curl -s "$SERVER_URL/mcp/capabilities" | jq '.' || echo "Failed to get capabilities"

# Test 3: MCP Initialize
echo ""
echo "3️⃣ MCP Initialize Request:"
curl -s -X POST "$SERVER_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }' | jq '.' || echo "Failed initialize request"

# Test 4: Tools List
echo ""
echo "4️⃣ Tools List:"
curl -s -X POST "$SERVER_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list"
  }' | jq '.' || echo "Failed tools/list request"

# Test 5: Ping
echo ""
echo "5️⃣ Ping Test:"
curl -s -X POST "$SERVER_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "ping"
  }' | jq '.' || echo "Failed ping request"

echo ""
echo "🏆 Test completed!"
echo ""
echo "💡 If you see empty responses, check the server logs for DEBUG messages."
echo "💡 Look for '🔍 DEBUG:' messages to see what's happening internally."
