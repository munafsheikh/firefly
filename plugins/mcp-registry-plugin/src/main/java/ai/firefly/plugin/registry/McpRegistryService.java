package ai.firefly.plugin.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class McpRegistryService {

    private final McpRegistryStore store;

    public McpRegistry register(McpRegistry mcp) {
        return store.register(mcp);
    }

    public void unregister(String id) {
        store.unregister(id);
    }

    public Optional<McpRegistry> getMcp(String id) {
        return store.getMcp(id);
    }

    public List<McpRegistry> listAllMcps() {
        return store.listAllMcps();
    }

    public McpRegistry updateMcp(McpRegistry mcp) {
        return store.updateMcp(mcp);
    }

    public McpRegistry refresh(String id) {
        Optional<McpRegistry> mcp = store.getMcp(id);
        if (mcp.isEmpty()) {
            throw new RuntimeException("MCP not found: " + id);
        }

        return mcp.get();
    }
}
