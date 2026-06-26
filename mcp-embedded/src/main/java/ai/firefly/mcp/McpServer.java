package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;

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
