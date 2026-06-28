#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:17922"
TIMEOUT=5

echo -e "${BLUE}🧠 Firefly MCP (Model Context Protocol) Test${NC}"
echo "=============================================="

# Check if app is running
echo -e "\n${YELLOW}Checking if app is running on port 17922...${NC}"
if ! curl -s -m $TIMEOUT "${BASE_URL}/" > /dev/null 2>&1; then
    echo -e "${RED}✗ App is not running on port 17922${NC}"
    echo "Start it with: docker compose up app --build"
    exit 1
fi
echo -e "${GREEN}✓ App is running${NC}"

# Test 1: Dashboard endpoint
echo -e "\n${BLUE}Test 1: Dashboard (HTML)${NC}"
if curl -s -m $TIMEOUT "${BASE_URL}/" | grep -q "Firefly"; then
    echo -e "${GREEN}✓ Dashboard accessible and contains 'Firefly'${NC}"
else
    echo -e "${RED}✗ Dashboard test failed${NC}"
fi

# Test 2: Swagger/OpenAPI
echo -e "\n${BLUE}Test 2: Swagger/OpenAPI${NC}"
if curl -s -m $TIMEOUT "${BASE_URL}/v3/api-docs" | grep -q "openapi"; then
    echo -e "${GREEN}✓ OpenAPI docs available${NC}"
else
    echo -e "${RED}✗ OpenAPI test failed${NC}"
fi

# Test 3: Dashboard info panel with MCP section
echo -e "\n${BLUE}Test 3: Dashboard MCP info panel${NC}"
if curl -s -m $TIMEOUT "${BASE_URL}/" | grep -q "Model Context Protocol"; then
    echo -e "${GREEN}✓ Dashboard info panel shows MCP section${NC}"
else
    echo -e "${RED}✗ MCP info panel not found on dashboard${NC}"
fi

# Test 4: MCP Registry endpoints
echo -e "\n${BLUE}Test 4: MCP Registry Plugin endpoints${NC}"

# Test 4a: /api/mcp-registry/mcps (list registered MCPs)
echo "  4a. GET /api/mcp-registry/mcps"
MCP_LIST=$(curl -s -m $TIMEOUT "${BASE_URL}/api/mcp-registry/mcps" 2>/dev/null || echo "error")
if echo "$MCP_LIST" | grep -q "\[\]" || echo "$MCP_LIST" | grep -q "id"; then
    echo -e "     ${GREEN}✓ MCP registry list endpoint responds${NC}"
    # Show count if non-empty
    COUNT=$(echo "$MCP_LIST" | grep -o '"id"' | wc -l)
    if [ "$COUNT" -gt 0 ]; then
        echo -e "     ${GREEN}  (${COUNT} MCP(s) registered)${NC}"
    else
        echo -e "     ${YELLOW}  (empty registry - ready to register MCPs)${NC}"
    fi
else
    echo -e "     ${YELLOW}~ MCP Registry plugin may not be active (not required)${NC}"
fi

# Test 4b: Dashboard lists 4 embedded MCP tools
echo "  4b. Embedded MCP Tools (in dashboard)"
DASHBOARD=$(curl -s -m $TIMEOUT "${BASE_URL}/" 2>/dev/null)
TOOLS_COUNT=0
for tool in "ListPlugins" "DashboardHealth" "ActuatorData" "ListActuatorEndpoints"; do
    if echo "$DASHBOARD" | grep -q "$tool"; then
        TOOLS_COUNT=$((TOOLS_COUNT + 1))
    fi
done
if [ "$TOOLS_COUNT" -eq 4 ]; then
    echo -e "     ${GREEN}✓ All 4 embedded MCP tools listed on dashboard${NC}"
else
    echo -e "     ${YELLOW}~ Only $TOOLS_COUNT/4 tools found on dashboard${NC}"
fi

# Test 5: Terminal endpoint
echo -e "\n${BLUE}Test 5: Web Terminal (TUI)${NC}"
if curl -s -m $TIMEOUT "${BASE_URL}/terminal" | grep -q "xterm"; then
    echo -e "${GREEN}✓ Terminal UI accessible${NC}"
else
    echo -e "${YELLOW}~ Terminal endpoint exists but may be loading${NC}"
fi

# Test 6: Actuator (if plugin active)
echo -e "\n${BLUE}Test 6: Actuator Plugin (optional)${NC}"
HEALTH=$(curl -s -m $TIMEOUT "${BASE_URL}/actuator/health" 2>/dev/null || echo "error")
if echo "$HEALTH" | grep -q "UP\|DOWN"; then
    echo -e "${GREEN}✓ Actuator plugin active and responding${NC}"
else
    echo -e "${YELLOW}~ Actuator plugin may not be loaded (optional)${NC}"
fi

# Test 7: List installed plugins
echo -e "\n${BLUE}Test 7: Installed Plugins${NC}"
DASHBOARD=$(curl -s -m $TIMEOUT "${BASE_URL}/" 2>/dev/null)
if echo "$DASHBOARD" | grep -q "cli-plugin"; then
    echo -e "${GREEN}✓ CLI plugin detected${NC}"
fi
if echo "$DASHBOARD" | grep -q "actuator-plugin"; then
    echo -e "${GREEN}✓ Actuator plugin detected${NC}"
fi
if echo "$DASHBOARD" | grep -q "ado-plugin"; then
    echo -e "${GREEN}✓ ADO plugin detected${NC}"
fi

# Summary
echo -e "\n${YELLOW}========================================${NC}"
echo -e "${GREEN}MCP Tests Complete!${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "Access Firefly in your browser:"
echo "  🌐 Dashboard:      ${BASE_URL}"
echo "  🧠 MCP Registry:   ${BASE_URL}/api/mcp-registry/mcps"
echo "  🧠 MCP Registry UI: ${BASE_URL}/pages/mcp-registry"
echo "  💻 Terminal UI:    ${BASE_URL}/terminal"
echo "  📚 Swagger:        ${BASE_URL}/swagger-ui.html"
echo "  🏥 Actuator:       ${BASE_URL}/actuator"
echo ""
echo "Dashboard displays 4 embedded MCP tools:"
echo "  • ListPlugins — Discover installed plugins"
echo "  • DashboardHealth — Real-time system status"
echo "  • ActuatorData — Fetch metrics, health, info"
echo "  • ListActuatorEndpoints — Browse actuator endpoints"
echo ""
