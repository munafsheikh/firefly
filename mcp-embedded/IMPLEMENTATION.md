# MCP Server Embedded in Firefly — Implementation Spec

**Branch**: `mcp-embedded` (isolated worktree)  
**Deadline**: Midnight (2026-06-26 23:59:59)  
**Scope**: All 3 phases (Phase 1 required; Phase 2 & 3 if time permits)  
**Language**: Java only (no Node.js)  
**Framework**: Spring Boot 4.0.6, Java 25  

---

## Overview

Embed a Model Context Protocol (MCP) server directly in Firefly's Spring Boot application. MCP server runs as a background thread on stdio, allowing Claude CLI to register and invoke Firefly tools without external processes.

**Reference Architecture**: See `/docs/MCP-ARCHITECTURE.md` (full design document)

---

## Phase 1: Embedded MCP Server (REQUIRED)

### 1.1 Add Dependencies to pom.xml

**File**: `pom.xml`

Add these dependencies (find existing dependency block, add to it):

```xml
<!-- Jackson for JSON-RPC handling (likely already present, verify) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Lombok for boilerplate reduction (already present, verify) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Verify**: Run `./mvnw dependency:tree | grep jackson` — should show jackson-databind.

---

### 1.2 Create MCP Properties Class

**File**: `src/main/java/ai/firefly/mcp/McpProperties.java`

```java
package ai.firefly.mcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firefly.mcp")
public class McpProperties {
    
    private boolean enabled = true;
    private String name = "firefly-mcp";
    private String version = "1.0.0";
    private String description = "Firefly Dashboard & Plugin Management via MCP";
    
    private int maxToolsDiscoveryTimeMs = 5000;
    private int toolInvocationTimeoutMs = 30000;
    private boolean debugLogging = false;
}
```

---

### 1.3 Create MCP Tool Interface

**File**: `src/main/java/ai/firefly/mcp/McpTool.java`

```java
package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Interface for tools exposed via MCP.
 * Implementations are Spring beans annotated with @McpTool or @Component.
 */
public interface McpTool {
    
    /**
     * Unique tool identifier (e.g., "firefly_list_plugins")
     */
    String getToolName();
    
    /**
     * Human-readable description
     */
    String getDescription();
    
    /**
     * JSON Schema describing input parameters.
     */
    Map<String, Object> getInputSchema();
    
    /**
     * Invoke the tool with provided arguments.
     *
     * @param arguments Parsed JSON arguments matching inputSchema
     * @return Result object (will be serialized to JSON)
     * @throws Exception If tool execution fails
     */
    Object invoke(JsonNode arguments) throws Exception;
}
```

---

### 1.4 Create MCP Tool Registry

**File**: `src/main/java/ai/firefly/mcp/McpToolRegistry.java`

```java
package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class McpToolRegistry {
    
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    
    public McpToolRegistry(ApplicationContext context) {
        context.getBeansOfType(McpTool.class).forEach((name, tool) -> {
            register(tool);
            log.info("Registered MCP tool: {}", tool.getToolName());
        });
    }
    
    public void register(McpTool tool) {
        tools.put(tool.getToolName(), tool);
    }
    
    public McpTool getTool(String toolName) {
        return tools.get(toolName);
    }
    
    public Collection<McpTool> getAllTools() {
        return tools.values();
    }
    
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}
```

---

### 1.5 Create MCP Server (Core Logic)

**File**: `src/main/java/ai/firefly/mcp/McpServer.java`

```java
package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * MCP Server implementation.
 * Handles JSON-RPC 2.0 protocol over stdio.
 */
@Slf4j
@Service
public class McpServer {
    
    private final McpToolRegistry toolRegistry;
    private final McpProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();
    
    private volatile boolean running = false;
    
    public McpServer(McpToolRegistry toolRegistry, McpProperties properties) {
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }
    
    public void start() {
        if (running) return;
        
        running = true;
        Thread serverThread = new Thread(this::runServer, "MCP-Server");
        serverThread.setDaemon(false);
        serverThread.start();
        
        log.info("MCP Server started: {} v{}", properties.getName(), properties.getVersion());
    }
    
    private void runServer() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(System.out, true)) {
            
            String line;
            while (running && (line = reader.readLine()) != null) {
                try {
                    JsonNode request = mapper.readTree(line);
                    JsonNode response = handleRequest(request);
                    writer.println(mapper.writeValueAsString(response));
                    writer.flush();
                } catch (Exception e) {
                    log.error("Error processing MCP request", e);
                    JsonNode error = error(null, -32603, "Internal error: " + e.getMessage());
                    writer.println(mapper.writeValueAsString(error));
                    writer.flush();
                }
            }
        } catch (IOException e) {
            log.error("MCP server fatal error", e);
            running = false;
        }
    }
    
    private JsonNode handleRequest(JsonNode request) {
        String method = request.path("method").asText();
        JsonNode id = request.get("id");
        JsonNode params = request.get("params");
        
        try {
            JsonNode result = switch (method) {
                case "initialize" -> handleInitialize(params);
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolCall(params);
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            };
            
            return response(id, result);
        } catch (Exception e) {
            log.warn("RPC method {} failed", method, e);
            return error(id, -32602, e.getMessage());
        }
    }
    
    private JsonNode handleInitialize(JsonNode params) {
        ObjectNode result = mapper.createObjectNode();
        result.put("name", properties.getName());
        result.put("version", properties.getVersion());
        
        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.putObject("tools");
        result.set("capabilities", capabilities);
        
        return result;
    }
    
    private JsonNode handleToolsList() {
        ObjectNode result = mapper.createObjectNode();
        var toolsArray = mapper.createArrayNode();
        
        for (McpTool tool : toolRegistry.getAllTools()) {
            ObjectNode toolDef = mapper.createObjectNode();
            toolDef.put("name", tool.getToolName());
            toolDef.put("description", tool.getDescription());
            toolDef.set("inputSchema", mapper.valueToTree(tool.getInputSchema()));
            toolsArray.add(toolDef);
        }
        
        result.set("tools", toolsArray);
        return result;
    }
    
    private JsonNode handleToolCall(JsonNode params) throws Exception {
        String toolName = params.path("name").asText();
        JsonNode arguments = params.get("arguments");
        
        McpTool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = tool.invoke(arguments);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Tool {} executed in {}ms", toolName, duration);
            
            ObjectNode response = mapper.createObjectNode();
            response.set("content", mapper.valueToTree(result));
            return response;
        } catch (Exception e) {
            log.error("Tool {} failed", toolName, e);
            throw e;
        }
    }
    
    private JsonNode response(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) response.set("id", id);
        response.set("result", result);
        return response;
    }
    
    private JsonNode error(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) response.set("id", id);
        
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        
        return response;
    }
    
    public void stop() {
        running = false;
        log.info("MCP Server stopped");
    }
}
```

---

### 1.6 Create MCP AutoConfiguration

**File**: `src/main/java/ai/firefly/mcp/McpAutoConfiguration.java`

```java
package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "firefly.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public McpToolRegistry mcpToolRegistry(org.springframework.context.ApplicationContext context) {
        return new McpToolRegistry(context);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public McpServer mcpServer(McpToolRegistry registry, McpProperties properties) {
        return new McpServer(registry, properties);
    }
    
    @Bean
    public McpServerLifecycle mcpServerLifecycle(McpServer server, McpProperties properties) {
        return new McpServerLifecycle(server, properties);
    }
}
```

---

### 1.7 Create MCP Server Lifecycle

**File**: `src/main/java/ai/firefly/mcp/McpServerLifecycle.java`

```java
package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Manages MCP server lifecycle: start when Spring context ready, stop on shutdown.
 */
@Slf4j
@Component
public class McpServerLifecycle {
    
    private final McpServer server;
    private final McpProperties properties;
    
    public McpServerLifecycle(McpServer server, McpProperties properties) {
        this.server = server;
        this.properties = properties;
    }
    
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (properties.isEnabled()) {
            server.start();
        }
    }
    
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        server.stop();
    }
}
```

---

### 1.8 Register AutoConfiguration

**File**: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Create if it doesn't exist. Add this line:

```
ai.firefly.mcp.McpAutoConfiguration
```

---

### 1.9 Update application.yaml

**File**: `src/main/resources/application.yaml`

Add (or update existing firefly section):

```yaml
firefly:
  mcp:
    enabled: true
    name: "firefly-mcp"
    version: "1.0.0"
    description: "Firefly Dashboard & Plugin Management"
    maxToolsDiscoveryTimeMs: 5000
    toolInvocationTimeoutMs: 30000
    debugLogging: false
```

---

### 1.10 Implement Tool 1: ListPluginsTool

**File**: `src/main/java/ai/firefly/mcp/tools/ListPluginsTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ListPluginsTool implements McpTool {
    
    private final DashboardService dashboardService;
    
    public ListPluginsTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @Override
    public String getToolName() {
        return "firefly_list_plugins";
    }
    
    @Override
    public String getDescription() {
        return "List all installed Firefly plugins with metadata (id, name, version, author)";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", new String[]{}
        );
    }
    
    @Override
    public Object invoke(JsonNode arguments) {
        return dashboardService.getInstalledPlugins();
    }
}
```

---

### 1.11 Implement Tool 2: GetDashboardHealthTool

**File**: `src/main/java/ai/firefly/mcp/tools/GetDashboardHealthTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GetDashboardHealthTool implements McpTool {
    
    private final DashboardService dashboardService;
    
    @Value("${server.port:17922}")
    private int serverPort;
    
    public GetDashboardHealthTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @Override
    public String getToolName() {
        return "firefly_get_dashboard_health";
    }
    
    @Override
    public String getDescription() {
        return "Get overall health status of Firefly dashboard including plugin availability";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", new String[]{}
        );
    }
    
    @Override
    public Object invoke(JsonNode arguments) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("version", "1.0.0");
        health.put("port", serverPort);
        health.put("pluginCount", dashboardService.getInstalledPlugins().size());
        health.put("actuatorAvailable", dashboardService.isActuatorAvailable());
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
```

---

### 1.12 Implement Tool 3: GetActuatorDataTool

**File**: `src/main/java/ai/firefly/mcp/tools/GetActuatorDataTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GetActuatorDataTool implements McpTool {
    
    private final DashboardService dashboardService;
    private final RestClient restClient = RestClient.create();
    
    public GetActuatorDataTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @Override
    public String getToolName() {
        return "firefly_get_actuator_data";
    }
    
    @Override
    public String getDescription() {
        return "Fetch data from a Spring Boot actuator endpoint (e.g., health, metrics, info)";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "endpoint", Map.of(
                    "type", "string",
                    "description", "Actuator endpoint name (e.g., 'health', 'metrics', 'env')"
                )
            ),
            "required", new String[]{"endpoint"}
        );
    }
    
    @Override
    public Object invoke(JsonNode arguments) throws Exception {
        if (!dashboardService.isActuatorAvailable()) {
            throw new IllegalStateException("Actuator not available");
        }
        
        String endpoint = arguments.path("endpoint").asText();
        String url = "http://localhost:17922/actuator/" + endpoint;
        
        try {
            return restClient.get()
                .uri(url)
                .retrieve()
                .body(Object.class);
        } catch (Exception e) {
            throw new Exception("Failed to fetch actuator endpoint: " + endpoint, e);
        }
    }
}
```

---

### 1.13 Implement Tool 4: ListActuatorEndpointsTool

**File**: `src/main/java/ai/firefly/mcp/tools/ListActuatorEndpointsTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ListActuatorEndpointsTool implements McpTool {
    
    private final DashboardService dashboardService;
    
    public ListActuatorEndpointsTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @Override
    public String getToolName() {
        return "firefly_list_actuator_endpoints";
    }
    
    @Override
    public String getDescription() {
        return "List available Spring Boot actuator endpoints (health, metrics, info, etc)";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", new String[]{}
        );
    }
    
    @Override
    public Object invoke(JsonNode arguments) throws Exception {
        if (!dashboardService.isActuatorAvailable()) {
            Map<String, String> result = new HashMap<>();
            result.put("available", "false");
            result.put("message", "Actuator plugin not loaded");
            return result;
        }
        
        Map<String, Object> endpoints = dashboardService.getActuatorEndpoints();
        return endpoints != null ? endpoints : new HashMap<>();
    }
}
```

---

### 1.14 Create Unit Tests

**File**: `src/test/java/ai/firefly/mcp/McpServerTest.java`

```java
package ai.firefly.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServerTest {
    
    @Autowired
    private McpToolRegistry registry;
    
    @Autowired
    private McpProperties properties;
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    void testMcpPropertiesLoaded() {
        assertTrue(properties.isEnabled());
        assertEquals("firefly-mcp", properties.getName());
        assertEquals("1.0.0", properties.getVersion());
    }
    
    @Test
    void testToolsRegistered() {
        assertTrue(registry.hasTool("firefly_list_plugins"));
        assertTrue(registry.hasTool("firefly_get_dashboard_health"));
        assertTrue(registry.hasTool("firefly_get_actuator_data"));
        assertTrue(registry.hasTool("firefly_list_actuator_endpoints"));
    }
    
    @Test
    void testListPluginsToolExists() {
        McpTool tool = registry.getTool("firefly_list_plugins");
        assertNotNull(tool);
        assertEquals("firefly_list_plugins", tool.getToolName());
    }
}
```

---

### 1.15 Verify & Build

Run these commands in order:

```bash
# Clean build
./mvnw clean install -q

# Run tests
./mvnw test -q

# Check for compilation errors
./mvnw compile -q && echo "✓ Compile successful"

# Verify MCP classes exist
find src/main/java/ai/firefly/mcp -name "*.java" | wc -l
# Should output: 8 (McpProperties, McpTool, McpToolRegistry, McpServer, McpAutoConfiguration, McpServerLifecycle, + 4 tool implementations)
```

---

## Phase 2: Internal MCP Registry (OPTIONAL, if time permits)

**Location**: `plugins/mcp-registry-plugin/`

Create a new Spring Boot plugin (following same pattern as `actuator-plugin`) that:

1. **Entity**: `McpRegistry` (id, name, type, executable, environment, tools[], enabled, version)
2. **Service**: `McpRegistryService` (CRUD operations, tool discovery, refresh)
3. **Persistence**: `McpRegistryStore` interface + `JsonMcpRegistryStore` implementation
4. **REST API**: `McpRegistryController` with endpoints:
   - `GET /api/mcp-registry/mcps` — list all
   - `POST /api/mcp-registry/mcps` — register new
   - `GET /api/mcp-registry/mcps/{id}` — get single
   - `DELETE /api/mcp-registry/mcps/{id}` — unregister
   - `POST /api/mcp-registry/mcps/{id}/refresh` — refresh tool capabilities
5. **Dashboard**: Add UI component to show registered MCPs

**Effort**: ~4 hours if Phase 1 complete

---

## Phase 3: Proxy MCP (OPTIONAL, if time permits)

**Location**: `plugins/mcp-proxy-plugin/` or standalone app

Create a Spring Boot component that:

1. **Reads** Phase 2 registry via REST API
2. **Discovers** tools from each registered external MCP
3. **Aggregates** all tools into single MCP interface
4. **Routes** tool invocations to correct MCP
5. **Caches** tool list, periodically refreshes

**Effort**: ~6 hours if Phases 1 & 2 complete

---

## Execution Checklist

### Phase 1 (REQUIRED — ~6 hours)

- [ ] Create worktree (`git worktree add -b mcp-embedded main`)
- [ ] Add dependencies to pom.xml
- [ ] Create McpProperties.java
- [ ] Create McpTool interface
- [ ] Create McpToolRegistry.java
- [ ] Create McpServer.java (core JSON-RPC handler)
- [ ] Create McpAutoConfiguration.java
- [ ] Create McpServerLifecycle.java
- [ ] Register auto-configuration in META-INF
- [ ] Update application.yaml
- [ ] Implement ListPluginsTool.java
- [ ] Implement GetDashboardHealthTool.java
- [ ] Implement GetActuatorDataTool.java
- [ ] Implement ListActuatorEndpointsTool.java
- [ ] Create McpServerTest.java
- [ ] Run: `./mvnw clean install`
- [ ] Verify: 4 tools registered in test output
- [ ] Commit: `git add -A && git commit -m "Phase 1: Embedded MCP server"`

### Phase 2 (OPTIONAL — ~4 hours if Phase 1 done)

- [ ] Create plugin structure: `plugins/mcp-registry-plugin/`
- [ ] Copy pom.xml from actuator-plugin, customize
- [ ] Create McpRegistry entity
- [ ] Create McpRegistryService
- [ ] Create McpRegistryStore interface + JsonMcpRegistryStore
- [ ] Create McpRegistryController (REST API)
- [ ] Create McpRegistryAutoConfiguration
- [ ] Update application.yaml with registry config
- [ ] Run: `./mvnw clean install`
- [ ] Commit: `git add -A && git commit -m "Phase 2: Internal MCP registry"`

### Phase 3 (OPTIONAL — ~6 hours if Phases 1 & 2 done)

- [ ] Create plugin/app structure: `plugins/mcp-proxy-plugin/`
- [ ] Create ToolAggregator (discover from Phase 2 registry)
- [ ] Create RequestRouter (match tool name → MCP)
- [ ] Create MCP client pool (stdio + HTTP clients)
- [ ] Create ProxyMcpServer
- [ ] Create ProxyMcpAutoConfiguration
- [ ] Run: `./mvnw clean install`
- [ ] Commit: `git add -A && git commit -m "Phase 3: Unified MCP proxy gateway"`

---

## Build & Test Commands

```bash
# Full build (all modules)
./mvnw clean install

# Core app only
./mvnw -pl . clean install

# Run with Docker Compose
docker compose up app --build

# Verify MCP server is loaded
docker compose up app --build 2>&1 | grep -i "mcp server"

# Manual test (if you have Claude CLI)
claude mcp add firefly -- java -jar target/firefly-*.jar
claude ask "List my Firefly plugins"
```

---

## Success Criteria

### Phase 1
- ✓ Code compiles without errors
- ✓ All 4 tools registered and discoverable
- ✓ Tests pass: `./mvnw test`
- ✓ MCP server starts when Firefly starts
- ✓ Claude CLI can register and invoke tools

### Phase 2
- ✓ Registry plugin loads alongside core app
- ✓ REST API endpoints functional
- ✓ Can register/list/unregister MCPs
- ✓ Dashboard shows registry status

### Phase 3
- ✓ Unified proxy discovers tools from registry
- ✓ Single MCP endpoint aggregates multiple MCPs
- ✓ Tool invocation routes to correct upstream MCP

---

## Notes for Copilot

1. **Stay in Java**: All code is Java. No Node.js, Python, or shell scripts (except tests).
2. **Follow Spring patterns**: Use @Component, @Service, @AutoConfiguration following existing code.
3. **Match style**: Look at `TerminalAutoConfiguration`, `DashboardService` for patterns.
4. **Test as you go**: Run `./mvnw test` after each major section.
5. **Commit often**: Each phase gets its own commit with clear message.
6. **Deadline**: Midnight 2026-06-26. Phase 1 is hard requirement; 2 & 3 are bonus.
7. **If stuck**: Check existing plugin code (actuator-plugin, ado-plugin) for reference.

---

## References

- Full design: `/docs/MCP-ARCHITECTURE.md`
- Firefly conventions: `/CLAUDE.md`
- Spring Boot docs: https://spring.io/projects/spring-boot
- MCP spec: https://modelcontextprotocol.io/
