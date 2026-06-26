package ai.firefly.plugin.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMcpRegistryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void testRegister() {
        JsonMcpRegistryStore store = newStore();
        McpRegistry mcp = sampleMcp("mcp-1");

        store.register(mcp);

        assertThat(Files.exists(storePath())).isTrue();
        assertThat(store.getMcp("mcp-1")).contains(mcp);
    }

    @Test
    void testUnregister() {
        JsonMcpRegistryStore store = newStore();
        store.register(sampleMcp("mcp-1"));

        store.unregister("mcp-1");

        assertThat(store.getMcp("mcp-1")).isEmpty();
        assertThat(store.listAllMcps()).isEmpty();
    }

    @Test
    void testGetMcp() {
        JsonMcpRegistryStore store = newStore();
        McpRegistry mcp = sampleMcp("mcp-1");
        store.register(mcp);

        assertThat(store.getMcp("mcp-1")).contains(mcp);
    }

    @Test
    void testListAllMcps() {
        JsonMcpRegistryStore store = newStore();
        McpRegistry first = sampleMcp("mcp-1");
        McpRegistry second = sampleMcp("mcp-2");
        store.register(first);
        store.register(second);

        assertThat(store.listAllMcps()).containsExactly(first, second);
    }

    private JsonMcpRegistryStore newStore() {
        McpRegistryProperties properties = new McpRegistryProperties();
        properties.setStorePath(storePath().toString());
        return new JsonMcpRegistryStore(properties);
    }

    private Path storePath() {
        return tempDir.resolve("registry.json");
    }

    private McpRegistry sampleMcp(String id) {
        return new McpRegistry(
            id,
            "Sample " + id,
            McpServerType.STDIO,
            "/opt/" + id,
            Map.of("K", "V"),
            List.of(),
            true,
            "1.0.0"
        );
    }
}
