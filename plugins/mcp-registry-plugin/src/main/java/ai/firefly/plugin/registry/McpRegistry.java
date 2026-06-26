package ai.firefly.plugin.registry;

import java.util.List;
import java.util.Map;

public record McpRegistry(
    String id,
    String name,
    McpServerType type,
    String executable,
    Map<String, String> environment,
    List<ToolCapability> tools,
    boolean enabled,
    String version
) {

    public McpRegistry {
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}

enum McpServerType {
    STDIO,
    HTTP
}
