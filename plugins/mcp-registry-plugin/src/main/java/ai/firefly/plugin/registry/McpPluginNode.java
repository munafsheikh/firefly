package ai.firefly.plugin.registry;

import java.util.List;

/**
 * A group node in the MCP tree: one entry per plugin that declares skills and/or
 * MCP server definitions, with those declarations listed as child nodes.
 */
public record McpPluginNode(
    String pluginId,
    String pluginName,
    String pluginVersion,
    List<SkillDefinition> skills,
    List<McpServerNodeDefinition> servers
) {

    public McpPluginNode {
        skills = skills == null ? List.of() : List.copyOf(skills);
        servers = servers == null ? List.of() : List.copyOf(servers);
    }
}
