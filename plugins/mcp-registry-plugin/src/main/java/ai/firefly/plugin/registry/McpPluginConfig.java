package ai.firefly.plugin.registry;

import java.util.Map;

/**
 * The resolved {@code firefly.plugin.<pluginId>.*} configuration for a plugin's node in the
 * MCP tree, shown in the "Settings" panel for that plugin's skills/servers.
 */
public record McpPluginConfig(
    String pluginId,
    String pluginName,
    String pluginVersion,
    String pluginDescription,
    Map<String, Object> properties
) {

    public McpPluginConfig {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }
}
