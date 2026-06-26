package ai.firefly.plugin.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpRegistryServiceTest {

    @Mock
    private McpRegistryStore store;

    @InjectMocks
    private McpRegistryService service;

    @Test
    void registerDelegatesToStore() {
        McpRegistry mcp = sampleMcp();
        when(store.register(mcp)).thenReturn(mcp);

        McpRegistry result = service.register(mcp);

        assertThat(result).isEqualTo(mcp);
        verify(store).register(mcp);
    }

    @Test
    void unregisterDelegatesToStore() {
        service.unregister("mcp-1");

        verify(store).unregister("mcp-1");
    }

    @Test
    void getMcpDelegatesToStore() {
        McpRegistry mcp = sampleMcp();
        when(store.getMcp("mcp-1")).thenReturn(Optional.of(mcp));

        Optional<McpRegistry> result = service.getMcp("mcp-1");

        assertThat(result).contains(mcp);
        verify(store).getMcp("mcp-1");
    }

    @Test
    void listAllMcpsDelegatesToStore() {
        List<McpRegistry> mcps = List.of(sampleMcp());
        when(store.listAllMcps()).thenReturn(mcps);

        List<McpRegistry> result = service.listAllMcps();

        assertThat(result).containsExactlyElementsOf(mcps);
        verify(store).listAllMcps();
    }

    @Test
    void updateMcpDelegatesToStore() {
        McpRegistry mcp = sampleMcp();
        when(store.updateMcp(mcp)).thenReturn(mcp);

        McpRegistry result = service.updateMcp(mcp);

        assertThat(result).isEqualTo(mcp);
        verify(store).updateMcp(mcp);
    }

    @Test
    void refreshReturnsExistingMcp() {
        McpRegistry mcp = sampleMcp();
        when(store.getMcp("mcp-1")).thenReturn(Optional.of(mcp));

        McpRegistry result = service.refresh("mcp-1");

        assertThat(result).isEqualTo(mcp);
        verify(store).getMcp("mcp-1");
    }

    @Test
    void refreshThrowsWhenMissing() {
        when(store.getMcp("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("missing"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("MCP not found: missing");
    }

    private McpRegistry sampleMcp() {
        return new McpRegistry(
            "mcp-1",
            "Sample MCP",
            McpServerType.STDIO,
            "/opt/mcp",
            Map.of("A", "B"),
            List.of(),
            true,
            "1.0.0"
        );
    }
}
