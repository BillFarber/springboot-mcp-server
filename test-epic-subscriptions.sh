#!/bin/bash

# 🎸 Epic 2112-Style MCP Resource Subscription Demo! 🎸
# Like the Temples of Syrinx, but for real-time resource monitoring!

echo "🎸🔥 EPIC 2112 MCP SUBSCRIPTION DEMO 🔥🎸"
echo "=============================================="
echo ""

# Colors for epic output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test data
URI1="file://temples-of-syrinx.java"
URI2="file://overture.ts"
CLIENT="vscode-2112"

echo -e "${PURPLE}🎵 Starting the epic subscription symphony...${NC}"
echo ""

# Start the server in background
echo -e "${BLUE}🚀 Launching MCP Server...${NC}"
java -jar build/libs/anthropicMCP-0.0.1-SNAPSHOT.jar stdio &
SERVER_PID=$!
sleep 2

echo -e "${GREEN}✅ Server launched with PID: $SERVER_PID${NC}"
echo ""

# Function to send MCP request
send_request() {
    local request="$1"
    local description="$2"
    
    echo -e "${YELLOW}📡 $description${NC}"
    echo "Request: $request"
    echo "$request" | java -jar build/libs/anthropicMCP-0.0.1-SNAPSHOT.jar stdio | head -n 20
    echo ""
    echo "---"
    echo ""
}

# 1. Subscribe to first resource
send_request '{"jsonrpc":"2.0","id":"sub1","method":"resources/subscribe","params":{"uri":"'$URI1'","clientId":"'$CLIENT'"}}' "🎸 Subscribing to Temples of Syrinx..."

# 2. Subscribe to second resource  
send_request '{"jsonrpc":"2.0","id":"sub2","method":"resources/subscribe","params":{"uri":"'$URI2'","clientId":"'$CLIENT'"}}' "🎸 Subscribing to Overture..."

# 3. List subscriptions
send_request '{"jsonrpc":"2.0","id":"list1","method":"resources/list_subscriptions","params":{}}' "📋 Listing all epic subscriptions..."

# 4. Simulate resource update
send_request '{"jsonrpc":"2.0","id":"update1","method":"resources/simulate_update","params":{"uri":"'$URI1'"}}' "🔥 Simulating update to Temples of Syrinx..."

# 5. Test completion with subscription context
send_request '{"jsonrpc":"2.0","id":"complete1","method":"completion/complete","params":{"text":"// Epic 2112 subscription for ","position":28}}' "🧠 Testing AI completion with subscription context..."

echo -e "${PURPLE}🎵 Epic subscription demo complete! 🎵${NC}"
echo -e "${CYAN}🎸 Rock on with real-time resource monitoring! 🎸${NC}"

# Clean up
kill $SERVER_PID 2>/dev/null
