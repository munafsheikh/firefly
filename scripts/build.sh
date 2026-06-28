#!/bin/bash
set -e

echo "🔥 Firefly Build Script"
echo "======================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Build core app
echo -e "\n${BLUE}Step 1: Building core app (runs tests)${NC}"
docker compose --profile build run --rm app-build
echo -e "${GREEN}✓ Core app built${NC}"

# Step 2: Build all plugins
echo -e "\n${BLUE}Step 2: Building all plugins${NC}"
docker compose --profile build run --rm plugin-build
echo -e "${GREEN}✓ All plugins built${NC}"

# Step 3: Copy plugins to plugins/ directory
echo -e "\n${BLUE}Step 3: Staging plugins for runtime${NC}"
find plugins -name "*.jar" -path "*/target/*.jar" -exec cp {} plugins/ \; 2>/dev/null || true
echo -e "${GREEN}✓ Plugins staged${NC}"

# Summary
echo -e "\n${YELLOW}========================================${NC}"
echo -e "${GREEN}Build complete!${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "Next steps:"
echo "  1. Run the app:"
echo "     docker compose up app --build"
echo ""
echo "  2. Access in browser:"
echo "     Dashboard:  http://localhost:17922"
echo "     Terminal UI: http://localhost:17922/terminal"
echo "     Swagger:    http://localhost:17922/swagger-ui.html"
echo "     Actuator:   http://localhost:17922/actuator"
echo ""
