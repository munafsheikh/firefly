package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Interface for tools exposed via MCP.
 * Implementations are Spring beans annotated with @Component.
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
