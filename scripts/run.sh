#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔥 Firefly — Build & Run${NC}"
echo "=========================="

# Check if plugins are already built
if ! ls plugins/*.jar >/dev/null 2>&1; then
    echo -e "\n${YELLOW}No plugins found. Running build...${NC}"
    bash scripts/build.sh
else
    echo -e "\n${GREEN}✓ Plugins already built${NC}"
fi

# Run the app
echo -e "\n${BLUE}Starting app on port 17922...${NC}"
echo -e "${YELLOW}Ctrl+C to stop${NC}\n"
docker compose up app --build

