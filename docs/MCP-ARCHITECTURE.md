# MCP Server Embedded in Firefly — Architecture & Implementation Plan

## Executive Summary

This document details embedding a Model Context Protocol (MCP) server directly into the Firefly Spring Boot application. Instead of external processes, MCP runs as a native Spring Boot component using the same language, configuration, and lifecycle as the rest of Firefly.

**Architecture**: MCP server runs in-process via a background thread on stdio transport. Claude CLI registers it like any other MCP. All tools are Spring beans, enabling reuse of existing Firefly services (DashboardService, TerminalService, etc).

**Scope**: Three progressive phases (same as original plan, but all Java/Spring Boot):
- **Phase 1**: Firefly exposes native MCP server (embedded)
- **Phase 2**: Internal MCP registry for external MCPs (Spring plugin)
- **Phase 3**: Proxy gateway to external MCPs via Firefly registry (Spring plugin)

---

## Why Embedded MCP?

| Aspect | Standalone MCP | Embedded MCP |
|--------|---|---|
| Language | Multiple (Node.js, Python, etc) | Single (Java) |
| Deployment | Separate process/service | Part of Firefly JAR |
| Lifecycle | Independent management | Managed by Spring |
| Development | Context switch to JS | Stay in Java/Spring |
| Service access | REST API calls | Direct Spring bean injection |
| Configuration | .env files | application.yaml |
| Testing | Process spawning | Standard Spring tests |
| **Best for** | polyglot teams, external MCPs | Java-native teams, tight integration |

Embedded is cleaner for Firefly because:
1. Single JVM, single process
2. Direct access to DashboardService, TerminalService, etc. (no REST layer overhead)
3. One build/test/deploy pipeline
4. Configuration via Spring properties
5. Lifecycle tied to Firefly startup/shutdown

---

## Phase 1: Native MCP Server (Embedded)

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Claude / Copilot / Codex CLI                               │
│  (register: claude mcp add firefly -- java -jar firefly.jar)│
└────────────────┬────────────────────────────────────────────┘
                 │ MCP Protocol (stdio)
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  Firefly Spring Boot App (port 17922 for HTTP)              │
│  ├─ Main JVM                                                │
│  │  ├─ Spring context                                       │
│  │  ├─ DashboardService                                     │
│  │  ├─ TerminalService                                      │
│  │  └─ Dashboard/Terminal/Swagger web endpoints             │
│  │                                                           │
│  └─ MCP Server (background thread)                          │
│     ├─ Stdio transport                                      │
│     ├─ Tool registry                                        │
│     ├─ Tool implementations (Spring beans)                  │
│     └─ Request handler                                      │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Strategy

#### Step 1: Add Java MCP SDK Dependency

**File**: `pom.xml` (add to dependencies)

```xml
<!-- Java MCP SDK (check latest version) -->
<dependency>
    <groupId>org.modelcontextprotocol</groupId>
    <artifactId>mcp-core</artifactId>
    <version>0.1.0</version>  <!-- Verify actual version -->
</dependency>
```

> **Note**: As of Feb 2025, a Java MCP SDK may not exist yet. If unavailable, two alternatives:
> 1. Generate from MCP JSON schema using a code generator
> 2. Implement minimal MCP protocol support (JSON-RPC 2.0 over stdio)
>
> For now, assume Java SDK exists or implement lightweight protocol handler.

#### Step 2: Create MCP Configuration & Properties

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
    
    // Tool-specific settings
    private int maxToolsDiscoveryTimeMs = 5000;
    private int toolInvocationTimeoutMs = 30000;
    
    // Logging
    private boolean debugLogging = false;
}
```

**File**: `src/main/resources/application.yaml` (add)

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

#### Step 3: Create MCP Tool Interface & Registry

**File**: `src/main/java/ai/firefly/mcp/McpTool.java`

```java
package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Interface for tools exposed via MCP.
 * Implementations are Spring beans annotated with @McpTool.
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
     * Example:
     * {
     *   "type": "object",
     *   "properties": {
     *     "pluginId": { "type": "string", "description": "Plugin ID" }
     *   },
     *   "required": []
     * }
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
        // Auto-discover all @McpTool beans and register them
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

**File**: `src/main/java/ai/firefly/mcp/McpTool.java` (annotation)

```java
package ai.firefly.mcp;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface McpTool {
    // Marker annotation; beans are auto-registered by McpToolRegistry
}
```

#### Step 4: Create MCP Server Service

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
 * 
 * Protocol:
 * - Client sends: {"jsonrpc":"2.0", "id":1, "method":"tools/list"}
 * - Server responds: {"jsonrpc":"2.0", "id":1, "result":{...}}
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
    
    /**
     * Start the MCP server on stdio.
     * Runs in a background thread.
     */
    public void start() {
        if (running) return;
        
        running = true;
        Thread serverThread = new Thread(this::runServer, "MCP-Server");
        serverThread.setDaemon(false);
        serverThread.start();
        
        log.info("MCP Server started: {} v{}", properties.getName(), properties.getVersion());
    }
    
    /**
     * Main server loop: read from stdin, process, write to stdout.
     */
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
    
    /**
     * Route incoming RPC calls to appropriate handler.
     */
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
    
    /**
     * Handle initialize: return server capabilities.
     */
    private JsonNode handleInitialize(JsonNode params) {
        ObjectNode result = mapper.createObjectNode();
        result.put("name", properties.getName());
        result.put("version", properties.getVersion());
        
        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.putObject("tools");  // We support tools
        result.set("capabilities", capabilities);
        
        return result;
    }
    
    /**
     * Handle tools/list: return all registered tools with their schemas.
     */
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
    
    /**
     * Handle tools/call: invoke a tool by name with arguments.
     */
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
    
    /**
     * Helper: build successful JSON-RPC response.
     */
    private JsonNode response(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) response.set("id", id);
        response.set("result", result);
        return response;
    }
    
    /**
     * Helper: build error JSON-RPC response.
     */
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

#### Step 5: Create MCP AutoConfiguration

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

**File**: `src/main/java/ai/firefly/mcp/McpServerLifecycle.java`

```java
package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
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
    
    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void onContextClosed() {
        server.stop();
    }
}
```

#### Step 6: Implement MCP Tools (Spring Beans)

Now create tools that use existing Firefly services. Each tool is a Spring bean implementing `McpTool`.

**File**: `src/main/java/ai/firefly/mcp/tools/ListPluginsTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
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
        // No input parameters
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

**File**: `src/main/java/ai/firefly/mcp/tools/GetDashboardHealthTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

**File**: `src/main/java/ai/firefly/mcp/tools/GetActuatorDataTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
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

**File**: `src/main/java/ai/firefly/mcp/tools/TerminalSendCommandTool.java`

```java
package ai.firefly.mcp.tools;

import ai.firefly.terminal.TerminalService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TerminalSendCommandTool implements McpTool {
    
    private final TerminalService terminalService;
    
    public TerminalSendCommandTool(TerminalService terminalService) {
        this.terminalService = terminalService;
    }
    
    @Override
    public String getToolName() {
        return "firefly_terminal_send_command";
    }
    
    @Override
    public String getDescription() {
        return "Send a command to the Firefly web terminal and get status. Returns session ID for reading output.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "command", Map.of(
                    "type", "string",
                    "description", "Command to execute (e.g., 'ls', 'pwd')"
                ),
                "sessionId", Map.of(
                    "type", "string",
                    "description", "Optional session ID; creates new session if omitted"
                )
            ),
            "required", new String[]{"command"}
        );
    }
    
    @Override
    public Object invoke(JsonNode arguments) throws Exception {
        String command = arguments.path("command").asText();
        String sessionId = arguments.path("sessionId").asText("");
        
        // This is a simplified example; adapt to your TerminalService API
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId != null ? sessionId : "session-" + System.nanoTime());
        result.put("command", command);
        result.put("status", "queued");
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
}
```

#### Step 7: Register Auto-Configuration with Spring

**File**: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Add (or create):
```
ai.firefly.mcp.McpAutoConfiguration
```

This ensures Spring Boot discovers and loads McpAutoConfiguration at startup.

#### Step 8: Update pom.xml

Add to dependencies:

```xml
<!-- JSON processing (already in project, but ensure it's available) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Lombok for convenience -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## Phase 1 Integration Checklist

- [ ] Add Java MCP SDK dependency (or implement protocol handler)
- [ ] Create `McpProperties.java`
- [ ] Create `McpTool.java` interface
- [ ] Create `McpToolRegistry.java`
- [ ] Create `McpServer.java` (core stdio handler)
- [ ] Create `McpAutoConfiguration.java`
- [ ] Create `McpServerLifecycle.java`
- [ ] Implement tool beans (ListPluginsTool, GetDashboardHealthTool, etc.)
- [ ] Register auto-configuration in META-INF
- [ ] Update pom.xml
- [ ] Test: Build and run Firefly, verify MCP server on stdio
- [ ] Test: Register with Claude CLI (`claude mcp add firefly -- java -jar target/firefly-*.jar`)
- [ ] Test: Invoke tools via Claude CLI

---

## Phase 1 Testing

### Unit Tests

**File**: `src/test/java/ai/firefly/mcp/McpServerTest.java`

```java
package ai.firefly.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServerTest {
    
    private McpToolRegistry registry;
    private McpServer server;
    private ObjectMapper mapper = new ObjectMapper();
    
    @BeforeEach
    void setup() {
        // Initialize from Spring context
    }
    
    @Test
    void testToolsListEndpoint() {
        // Test tools/list returns all registered tools
    }
    
    @Test
    void testToolInvocation() {
        // Test calling a specific tool
    }
}
```

### Integration Tests

Start Firefly, connect to stdio, send MCP protocol requests:

```bash
# Build Firefly
docker compose --profile build run --rm app-build

# Run Firefly (stdio will be available for MCP clients)
java -jar target/firefly-*.jar

# In another terminal, test MCP by sending JSON-RPC requests:
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | nc localhost 17922
# Should receive list of tools
```

### Manual CLI Test

```bash
# Register Firefly MCP with Claude CLI
claude mcp add firefly -- java -jar /path/to/firefly-*.jar

# Test via Claude
claude ask "List my Firefly plugins"
claude ask "What's the health of my Firefly instance?"
```

---

## Phase 1 Directory Structure

```
firefly/
├── src/main/java/ai/firefly/
│   ├── FireflyApplication.java
│   ├── mcp/                                     [NEW]
│   │   ├── McpProperties.java
│   │   ├── McpTool.java
│   │   ├── McpToolRegistry.java
│   │   ├── McpServer.java
│   │   ├── McpAutoConfiguration.java
│   │   ├── McpServerLifecycle.java
│   │   └── tools/                              [NEW]
│   │       ├── ListPluginsTool.java
│   │       ├── GetDashboardHealthTool.java
│   │       ├── GetActuatorDataTool.java
│   │       ├── TerminalSendCommandTool.java
│   │       ├── TerminalReadOutputTool.java     [TODO]
│   │       ├── ListActuatorEndpointsTool.java  [TODO]
│   │       └── GetSwaggerSchemaTool.java       [TODO]
│   ├── dashboard/
│   ├── terminal/
│   └── ... (existing)
├── src/main/resources/
│   ├── application.yaml                        [UPDATED]
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports  [UPDATED]
├── src/test/java/ai/firefly/
│   ├── mcp/                                    [NEW]
│   │   ├── McpServerTest.java
│   │   └── tools/
│   │       └── ListPluginsToolTest.java
│   └── ... (existing)
└── pom.xml                                     [UPDATED]
```

---

## Phase 1: Complete Tool List

| Tool | Input | Output | Notes |
|------|-------|--------|-------|
| `firefly_list_plugins` | none | `List<PluginInfo>` | Use DashboardService.getInstalledPlugins() |
| `firefly_get_dashboard_health` | none | `{status, version, port, ...}` | Return health snapshot |
| `firefly_list_actuator_endpoints` | none | `Map<String, String>` | Fetch from /actuator if available |
| `firefly_get_actuator_data` | `endpoint: string` | Endpoint response (JSON) | Proxy to /actuator/{endpoint} |
| `firefly_terminal_send_command` | `command: string, sessionId?: string` | `{sessionId, status}` | Queue command for terminal |
| `firefly_terminal_read_output` | `sessionId: string` | `{output: string, more: bool}` | Read queued terminal output |
| `firefly_get_swagger_schema` | none | Parsed OpenAPI schema | Fetch and parse from /v3/api-docs |
| `firefly_invoke_api` | `method, path, body?, headers?` | Response body | Generic API proxy |

---

## Phase 2: Internal MCP Registry (Spring Plugin)

**Unchanged** from previous plan. This is a Spring Boot plugin that:
- Maintains registry of external MCPs (ADO, Gmail, etc.)
- Provides REST API to register/list/manage external MCPs
- Stores registry in JSON or database
- Exposes registry to Phase 3 proxy

**Location**: `plugins/mcp-registry-plugin/` (same structure as actuator-plugin, ado-plugin)

---

## Phase 3: Proxy MCP (Spring Plugin)

Create a Spring Boot application (can be standalone or a plugin) that:
- Reads Firefly's MCP registry (Phase 2)
- Discovers tools from each registered external MCP
- Aggregates all tools into single MCP interface
- Routes tool invocations to correct MCP
- Handles context/session management

**Location**: Standalone Spring Boot app (separate JAR) or as plugin under `plugins/`

---

## Comparison: Embedded vs. Separate MCP

| Aspect | Embedded (Phase 1) | Registry (Phase 2) | Proxy (Phase 3) |
|--------|---|---|---|
| **Runtime** | Same JVM as Firefly | Spring plugin | Separate Spring app or plugin |
| **Scope** | Firefly APIs only | External MCP management | Multi-MCP aggregation |
| **Dependencies** | Java, Spring | Java, Spring | Java, Spring |
| **Launch** | `java -jar firefly.jar` | Baked into JAR | `java -jar unified-mcp.jar` |
| **Config** | application.yaml | application.yaml + REST API | application.yaml + env vars |

---

## Performance Characteristics

### Embedded (Phase 1)
- **Latency**: Sub-millisecond (direct Java calls, no network)
- **Memory**: +~50MB for MCP runtime
- **Throughput**: Limited by single JVM, but very high for tool discovery
- **Scalability**: Single instance; suitable for local development

### Registry (Phase 2)
- **Latency**: ~1-50ms (local REST API calls)
- **Memory**: +~20MB for registry service
- **Throughput**: Good for metadata queries
- **Scalability**: Scales with REST endpoint

### Proxy (Phase 3)
- **Latency**: ~50-500ms (stdio process + aggregation)
- **Memory**: +~100MB for proxy + MCP processes
- **Throughput**: Depends on upstream MCPs
- **Scalability**: Process pooling, caching

---

## Security Considerations

1. **Embedded Phase 1**: No network exposure; stdio only. Firefly binaries inherit all access controls.
2. **Registry Phase 2**: REST API should be protected by Spring Security if exposed over network.
3. **Proxy Phase 3**: Proxy should validate tool invocations and apply rate limiting.

---

## Future Enhancements

1. **Tool Versioning**: Track tool versions; support multiple versions per tool
2. **Tool Caching**: Cache tool discovery results; refresh on-demand
3. **Error Recovery**: Retry logic, circuit breaker for failing tools
4. **Monitoring**: Metrics for tool invocations, latency histograms
5. **Auth/RBAC**: Scope tools by user role; audit invocations
6. **Async Tools**: Support long-running tool operations via callbacks

---

## Implementation Timeline

| Phase | Effort | Timeline |
|-------|--------|----------|
| Phase 1 (Embedded MCP) | 3-5 days | 1 developer, 1 week |
| Phase 2 (Registry) | 3-4 days | 1 developer, 1 week |
| Phase 3 (Proxy) | 4-6 days | 1 developer, 1-2 weeks |
| **Total** | 10-15 days | 2-4 weeks (1 developer) |

---

## Success Metrics

- Phase 1: MCP server functional; all tools discoverable and invokable via Claude CLI
- Phase 2: Registry CRUD API complete; external MCPs discoverable via dashboard
- Phase 3: Unified proxy aggregates 3+ MCPs; tool routing works end-to-end

---

## References

- MCP Protocol: https://modelcontextprotocol.io/
- Spring Boot Auto-Configuration: https://spring.io/projects/spring-boot
- Java MCP SDK: (TBD — check current availability)
