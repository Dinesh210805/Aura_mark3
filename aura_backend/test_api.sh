#!/bin/bash

# AURA Backend Agent Test Script

echo "🧪 Testing AURA Backend Agent..."

# Base URL
BASE_URL="http://localhost:8000"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test functions
test_health() {
    echo -e "${YELLOW}Testing health endpoint...${NC}"
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/health")
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✅ Health check passed${NC}"
    else
        echo -e "${RED}❌ Health check failed (HTTP $response)${NC}"
    fi
}

test_root() {
    echo -e "${YELLOW}Testing root endpoint...${NC}"
    response=$(curl -s "$BASE_URL/")
    if echo "$response" | grep -q "AURA Backend Agent"; then
        echo -e "${GREEN}✅ Root endpoint working${NC}"
    else
        echo -e "${RED}❌ Root endpoint failed${NC}"
    fi
}

test_chat() {
    echo -e "${YELLOW}Testing chat endpoint...${NC}"
    response=$(curl -s -X POST "$BASE_URL/chat" \
        -H "Content-Type: application/json" \
        -d '{"text": "Hello, can you help me?", "session_id": "test"}')
    
    if echo "$response" | grep -q "success"; then
        echo -e "${GREEN}✅ Chat endpoint working${NC}"
    else
        echo -e "${RED}❌ Chat endpoint failed${NC}"
        echo "Response: $response"
    fi
}

test_graph_info() {
    echo -e "${YELLOW}Testing graph info endpoint...${NC}"
    response=$(curl -s "$BASE_URL/graph/info")
    if echo "$response" | grep -q "nodes"; then
        echo -e "${GREEN}✅ Graph info endpoint working${NC}"
    else
        echo -e "${RED}❌ Graph info endpoint failed${NC}"
    fi
}

# Check if server is running
echo "Checking if server is running at $BASE_URL..."
if ! curl -s "$BASE_URL/" > /dev/null; then
    echo -e "${RED}❌ Server is not running at $BASE_URL${NC}"
    echo "Please start the server with: python run.py"
    exit 1
fi

echo -e "${GREEN}✅ Server is running${NC}"
echo ""

# Run tests
test_root
test_health
test_graph_info
test_chat

echo ""
echo -e "${YELLOW}📊 Test Results Summary:${NC}"
echo "- All basic endpoints tested"
echo "- For full testing, upload audio/image files to /process endpoint"
echo ""
echo -e "${YELLOW}💡 Next Steps:${NC}"
echo "1. Test with actual audio files using the /process endpoint"
echo "2. Check logs for detailed processing information"
echo "3. Visit http://localhost:8000/docs for interactive API testing"
