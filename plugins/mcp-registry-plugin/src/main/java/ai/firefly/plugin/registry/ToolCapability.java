package ai.firefly.plugin.registry;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolCapability(
    String name,
    String description,
    JsonNode inputSchema
) {
}
