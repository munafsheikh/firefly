package ai.firefly.plugin.registry;

import java.util.List;
import java.util.Optional;

public interface McpRegistryStore {

    McpRegistry register(McpRegistry mcp);

    void unregister(String id);

    Optional<McpRegistry> getMcp(String id);

    List<McpRegistry> listAllMcps();

    McpRegistry updateMcp(McpRegistry mcp);
}
