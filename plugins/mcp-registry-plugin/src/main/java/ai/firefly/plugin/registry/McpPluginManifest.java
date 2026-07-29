package ai.firefly.plugin.registry;

import java.util.List;

/**
 * Deserialized form of a plugin-bundled {@code META-INF/firefly/mcp-plugin.json} resource:
 * the skills and/or MCP server definitions that plugin contributes to the MCP tree.
 */
public record McpPluginManifest(
    List<SkillDefinition> skills,
    List<McpServerNodeDefinition> servers
) {

    public McpPluginManifest {
        skills = skills == null ? List.of() : List.copyOf(skills);
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public boolean isEmpty() {
        return skills.isEmpty() && servers.isEmpty();
    }
}
