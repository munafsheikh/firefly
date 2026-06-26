# Handoff to Copilot: Implement Embedded MCP Server in Firefly

**Branch**: `mcp-embedded` (isolated worktree)  
**Deadline**: 2026-06-26 23:59:59 (midnight)  
**Priority**: Phase 1 (REQUIRED) → Phase 2 (if time) → Phase 3 (if time)  
**Language**: Java only (Spring Boot 4.0.6, Java 25)

---

## Quick Start

1. Read this entire file
2. Read `IMPLEMENTATION.md` (detailed spec with all code)
3. Execute checklist in order
4. Commit after each phase
5. Report when done

---

## What to Implement

### Phase 1: Embedded MCP Server (6-7 hours, REQUIRED)

Embed a JSON-RPC 2.0 over stdio MCP server directly in Firefly Spring Boot app. No external processes.

**Key deliverables**:
- McpServer.java (core protocol handler)
- 4 tool implementations (list plugins, health, actuator data, list endpoints)
- Spring auto-config + lifecycle management
- Tests

**Why embedded**: Single JVM, direct Spring bean access, zero overhead, no subprocess management.

### Phase 2: Internal MCP Registry (4 hours, OPTIONAL)

Spring Boot plugin that maintains a registry of external MCPs (ADO, Gmail, etc) with REST API.

**Deliverables**:
- Registry entity, service, persistence layer
- REST CRUD API
- Dashboard integration

**Skip if time runs out**: Phase 1 alone is fully functional.

### Phase 3: Unified Proxy Gateway (6 hours, OPTIONAL)

Standalone Spring component that reads Phase 2 registry and proxies to multiple MCPs.

**Deliverables**:
- Tool aggregator (discovers tools from all MCPs)
- Request router (matches tool → MCP)
- Proxy MCP server

**Skip if time runs out**: Phases 1 & 2 are sufficient.

---

## Current State

- **Worktree**: `/home/m/code/github/munafsheikh/firefly/mcp-embedded/` (clean, on `mcp-embedded` branch)
- **Project**: Spring Boot 4.0.6, Java 25, plugin-based architecture
- **Key files read**: CLAUDE.md, pom.xml, DashboardService, TerminalAutoConfiguration
- **Design doc**: `docs/MCP-ARCHITECTURE.md` (full reference)
- **Spec**: `IMPLEMENTATION.md` (complete with all code snippets)

---

## How to Execute

### Step 1: Understand the Architecture (10 min)

Read section "Phase 1: Embedded MCP Server" in `IMPLEMENTATION.md`.

Key concepts:
- **McpServer**: Reads from stdin, parses JSON-RPC, dispatches to tools, writes response to stdout
- **McpTool interface**: Each tool is a Spring bean implementing this
- **McpToolRegistry**: Auto-discovers all McpTool beans at startup
- **McpAutoConfiguration**: Spring Boot auto-config, conditional on property
- **Lifecycle**: McpServerLifecycle starts server when Spring context ready

### Step 2: Create MCP Core Classes (2-3 hours)

Follow checklist in `IMPLEMENTATION.md` sections 1.1–1.9:

1. ✓ Add dependencies to pom.xml
2. ✓ Create `src/main/java/ai/firefly/mcp/McpProperties.java`
3. ✓ Create `src/main/java/ai/firefly/mcp/McpTool.java`
4. ✓ Create `src/main/java/ai/firefly/mcp/McpToolRegistry.java`
5. ✓ Create `src/main/java/ai/firefly/mcp/McpServer.java` (copy full code from spec)
6. ✓ Create `src/main/java/ai/firefly/mcp/McpAutoConfiguration.java`
7. ✓ Create `src/main/java/ai/firefly/mcp/McpServerLifecycle.java`
8. ✓ Create `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with `ai.firefly.mcp.McpAutoConfiguration`
9. ✓ Update `src/main/resources/application.yaml`

**Build check** after this section:

```bash
./mvnw clean compile -q && echo "✓ Phase 1 core classes compile"
```

### Step 3: Implement Tools (2-3 hours)

Create 4 tool bean implementations (sections 1.10–1.13 of spec):

1. ✓ ListPluginsTool — uses DashboardService.getInstalledPlugins()
2. ✓ GetDashboardHealthTool — returns health snapshot
3. ✓ GetActuatorDataTool — proxies to /actuator/{endpoint}
4. ✓ ListActuatorEndpointsTool — lists available endpoints

Each is a `@Component` implementing `McpTool`.

**Build check**:

```bash
./mvnw clean compile -q && echo "✓ Tools compile"
```

### Step 4: Create Tests & Verify (1-2 hours)

1. ✓ Create `src/test/java/ai/firefly/mcp/McpServerTest.java` (from spec section 1.14)
2. ✓ Run: `./mvnw clean test`
3. ✓ Verify 4 tools are registered

Expected test output:
```
testMcpPropertiesLoaded: PASS
testToolsRegistered: PASS (4 tools)
testListPluginsToolExists: PASS
...
BUILD SUCCESS
```

### Step 5: Final Phase 1 Verification

```bash
# Full clean build
./mvnw clean install -q

# Confirm classes exist
find src/main/java/ai/firefly/mcp -type f -name "*.java" | wc -l
# Should output: 8 (McpProperties, McpTool, McpToolRegistry, McpServer, McpAutoConfiguration, McpServerLifecycle, + 4 tools)

# Check git status
git status --short
# Should show only Phase 1 files

# Commit Phase 1
git add -A
git commit -m "Phase 1: Embedded MCP server with 4 tools"
```

### Step 6: Phase 2 (if time: 3-4 hours)

If it's before ~8 PM, start Phase 2:

1. Create `plugins/mcp-registry-plugin/` directory
2. Copy `plugins/actuator-plugin/pom.xml` as template, customize for registry
3. Implement McpRegistry entity, service, store, controller (see `IMPLEMENTATION.md` section 2)
4. Build: `./mvnw clean install`
5. Test via REST API (e.g., `curl http://localhost:17922/api/mcp-registry/mcps`)
6. Commit: `git add -A && git commit -m "Phase 2: Internal MCP registry"`

### Step 7: Phase 3 (if time: 5-6 hours)

If it's before ~10 PM, start Phase 3:

1. Create `plugins/mcp-proxy-plugin/` or standalone app
2. Implement tool aggregator, request router, MCP client pool
3. Create proxy MCP server that reads Phase 2 registry
4. Build: `./mvnw clean install`
5. Test: Verify proxy aggregates tools from multiple MCPs
6. Commit: `git add -A && git commit -m "Phase 3: Unified MCP proxy gateway"`

---

## Testing & Validation

### After Phase 1 (REQUIRED)

**Unit tests**:
```bash
./mvnw test -Dtest=McpServerTest -q
```

Expected: 3 tests pass
- testMcpPropertiesLoaded
- testToolsRegistered
- testListPluginsToolExists

**Build verification**:
```bash
./mvnw clean install -q && echo "BUILD OK"
```

**Manual test (optional, if Claude CLI available)**:
```bash
java -jar target/firefly-*.jar &
# Firefly starts, MCP server runs on stdio
sleep 3

# In another terminal:
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | nc localhost 17922
# Should output: JSON with list of tools
```

### After Phase 2 (if implemented)

**REST API test**:
```bash
curl http://localhost:17922/api/mcp-registry/mcps
# Should output: empty array or list of registered MCPs
```

### After Phase 3 (if implemented)

**Proxy test**:
```bash
# Unified proxy running
curl http://localhost:17923/api/mcp/tools
# Should output: aggregated tools from all MCPs
```

---

## Key Files & Locations

### Phase 1 (8 classes)
```
src/main/java/ai/firefly/mcp/
├── McpProperties.java
├── McpTool.java
├── McpToolRegistry.java
├── McpServer.java
├── McpAutoConfiguration.java
├── McpServerLifecycle.java
└── tools/
    ├── ListPluginsTool.java
    ├── GetDashboardHealthTool.java
    ├── GetActuatorDataTool.java
    └── ListActuatorEndpointsTool.java

src/test/java/ai/firefly/mcp/
└── McpServerTest.java

src/main/resources/
├── application.yaml (UPDATED)
└── META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports (CREATED)

pom.xml (UPDATED with Jackson deps)
```

### Phase 2 (optional)
```
plugins/mcp-registry-plugin/
├── pom.xml
└── src/main/java/ai/firefly/plugin/registry/
    ├── McpRegistryAutoConfiguration.java
    ├── McpRegistryProperties.java
    ├── McpRegistry.java (entity)
    ├── ToolCapability.java
    ├── McpRegistryService.java
    ├── McpRegistryStore.java
    ├── JsonMcpRegistryStore.java
    ├── McpRegistryController.java
    └── McpRegistryPageController.java
```

### Phase 3 (optional)
```
plugins/mcp-proxy-plugin/
├── pom.xml
└── src/main/java/ai/firefly/plugin/proxy/
    ├── ProxyMcpAutoConfiguration.java
    ├── ToolAggregator.java
    ├── RequestRouter.java
    ├── McpClientPool.java
    └── ProxyMcpServer.java
```

---

## Style & Conventions

Copy from existing code:

- **Annotations**: Use `@Slf4j` (Lombok), `@Component`, `@Service`, `@AutoConfiguration`
- **Package structure**: `ai.firefly.mcp.*` (Phase 1), `ai.firefly.plugin.registry.*` (Phase 2), etc.
- **Naming**: camelCase for methods, PascalCase for classes, UPPER_CASE for constants
- **Comments**: Minimal; use JavaDoc for public APIs
- **Error handling**: Throw checked exceptions, log at WARN/ERROR level
- **Testing**: Use JUnit 5, Spring Boot Test, AssertJ assertions

See `src/main/java/ai/firefly/terminal/TerminalAutoConfiguration.java` for reference.

---

## Time Budget

| Phase | Effort | ETA |
|-------|--------|-----|
| Phase 1 (core) | 6-7 hours | Now → ~6 PM |
| Phase 2 (registry) | 3-4 hours | ~6 PM → ~8 PM |
| Phase 3 (proxy) | 5-6 hours | ~8 PM → ~11 PM |
| Buffer | 1 hour | ~11 PM → Midnight |

**Deadline**: Midnight 2026-06-26  
**Time now**: ~5 PM  
**Hard stop**: 11:45 PM (ensure commits are pushed)

---

## Commits

Commit after each phase:

```bash
# Phase 1
git add -A
git commit -m "Phase 1: Embedded MCP server with 4 tools"

# Phase 2 (if done)
git add -A
git commit -m "Phase 2: Internal MCP registry plugin"

# Phase 3 (if done)
git add -A
git commit -m "Phase 3: Unified MCP proxy gateway"

# Push to origin (important!)
git push -u origin mcp-embedded
```

---

## When Stuck

### Compile error
1. Read the error carefully
2. Check if dependency is in pom.xml
3. Run `./mvnw dependency:tree | grep <package>`
4. If needed, add to pom.xml and `mvnw clean compile`

### Test fails
1. Run single test: `./mvnw test -Dtest=McpServerTest#testToolsRegistered -q`
2. Check if tool bean is created (look for "Registered MCP tool" in logs)
3. Verify `application.yaml` has `firefly.mcp.enabled: true`

### Class not found
1. Check package name is correct
2. Verify file is in right directory
3. Run `./mvnw clean compile`

### Need reference
1. Check `IMPLEMENTATION.md` (complete code)
2. Look at `TerminalAutoConfiguration`, `DashboardService`, `TerminalService` for patterns
3. Check `plugins/actuator-plugin` structure for plugin pattern

---

## Checklist: Before Midnight

- [ ] Phase 1 core classes created & compiling
- [ ] Phase 1 tools implemented
- [ ] Phase 1 tests passing
- [ ] Phase 1 committed
- [ ] Phase 2 started (if time)
- [ ] Phase 2 committed (if done)
- [ ] Phase 3 started (if time)
- [ ] Phase 3 committed (if done)
- [ ] All code pushed to `mcp-embedded` branch
- [ ] Build passes: `./mvnw clean install -q`
- [ ] No uncommitted changes

---

## Success

When done, output:

```
COMPLETED
---------
Phase 1: [✓ DONE | ⏸ IN PROGRESS | ✗ NOT STARTED]
Phase 2: [✓ DONE | ⏸ IN PROGRESS | ✗ NOT STARTED]
Phase 3: [✓ DONE | ⏸ IN PROGRESS | ✗ NOT STARTED]

Commits:
  <list of commit hashes/messages>

Tests:
  Phase 1 unit tests: [PASS | FAIL]
  Build status: [SUCCESS | FAIL]

Files changed:
  <count> files in <count> commits
```

---

## Final Notes

- **You are not blocked**: All code is in `IMPLEMENTATION.md`. Copy-paste and adapt.
- **Test as you go**: Don't wait until midnight to test Phase 1. Test each section.
- **Focus on Phase 1**: It's self-contained and high-value. Phases 2 & 3 are bonus.
- **Commit often**: After Phase 1, commit. After Phase 2 (if done), commit. Etc.
- **Ask for help**: If stuck, review the spec, check existing plugins, read error messages carefully.

Good luck! 🚀
