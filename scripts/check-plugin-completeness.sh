#!/bin/bash
# Pre-merge documentation/testing completeness gate.
#
# Discovers every plugin under plugins/*/ (auto-covers new plugins added in a PR — nothing here
# needs updating when a plugin is added) and requires, for each:
#   1. A README.md in the plugin's root directory.
#   2. A META-INF/plugin.properties under src/main/resources (dashboard metadata).
#   3. At least one JUnit test class under src/test/java, UNLESS the plugin ships no Java source
#      at all under src/main/java (e.g. a pure-CSS theme plugin — nothing to unit test).
#
# Run from the repo root: ./scripts/check-plugin-completeness.sh
# Used by .github/workflows/pr-quality-gate.yml as part of the required pre-merge checks.
set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

failures=0
checked=0

echo "🔎 Checking plugin documentation/testing completeness..."
echo ""

for pom in plugins/*/pom.xml; do
    [ -f "$pom" ] || continue
    plugin_dir="$(dirname "$pom")"
    plugin_name="$(basename "$plugin_dir")"
    checked=$((checked + 1))
    plugin_ok=1

    if [ ! -f "$plugin_dir/README.md" ]; then
        echo -e "${RED}✗${NC} $plugin_name: missing README.md"
        plugin_ok=0
    fi

    if [ ! -f "$plugin_dir/src/main/resources/META-INF/plugin.properties" ]; then
        echo -e "${RED}✗${NC} $plugin_name: missing src/main/resources/META-INF/plugin.properties (dashboard metadata)"
        plugin_ok=0
    fi

    main_java_count=$(find "$plugin_dir/src/main/java" -name '*.java' 2>/dev/null | wc -l | tr -d ' ')
    if [ "$main_java_count" -gt 0 ]; then
        test_java_count=$(find "$plugin_dir/src/test/java" -name '*.java' 2>/dev/null | wc -l | tr -d ' ')
        if [ "$test_java_count" -eq 0 ]; then
            echo -e "${RED}✗${NC} $plugin_name: has $main_java_count main source file(s) but no test classes under src/test/java"
            plugin_ok=0
        fi
    fi

    if [ "$plugin_ok" -eq 1 ]; then
        echo -e "${GREEN}✓${NC} $plugin_name: documented and tested"
    else
        failures=$((failures + 1))
    fi
done

echo ""
if [ "$checked" -eq 0 ]; then
    echo -e "${YELLOW}No plugins found under plugins/*/pom.xml — nothing to check.${NC}"
    exit 0
fi

if [ "$failures" -gt 0 ]; then
    echo -e "${RED}$failures of $checked plugin(s) failed the completeness check.${NC}"
    echo "Every plugin needs a README.md, META-INF/plugin.properties, and (if it has Java source) at least one test class."
    exit 1
fi

echo -e "${GREEN}All $checked plugin(s) are documented and tested.${NC}"
