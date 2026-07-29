package ai.firefly.plugin.registry;

public record McpServerNodeDefinition(
    String name,
    String type,
    String target,
    String description
) {
}
