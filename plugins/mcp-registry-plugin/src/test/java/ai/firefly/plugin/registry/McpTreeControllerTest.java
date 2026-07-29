package ai.firefly.plugin.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class McpTreeControllerTest {

    @Mock
    private McpPluginTreeScanner scanner;

    @Mock
    private McpPluginConfigResolver configResolver;

    @InjectMocks
    private McpTreeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void treeReturnsGroupedNodes() throws Exception {
        McpPluginNode node = new McpPluginNode(
            "sample", "Sample Plugin", "1.0.0",
            List.of(new SkillDefinition("sample-skill", "desc")),
            List.of()
        );
        when(scanner.scanTree()).thenReturn(List.of(node));

        mockMvc.perform(get("/api/mcp-registry/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].pluginId").value("sample"))
            .andExpect(jsonPath("$[0].skills[0].name").value("sample-skill"));
    }

    @Test
    void configReturnsResolvedSettings() throws Exception {
        McpPluginConfig config = new McpPluginConfig("sample", "Sample Plugin", "1.0.0", "desc", Map.of("firefly.plugin.sample.foo", "bar"));
        when(configResolver.resolve("sample")).thenReturn(config);

        mockMvc.perform(get("/api/mcp-registry/tree/sample/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pluginId").value("sample"))
            .andExpect(jsonPath("$.properties['firefly.plugin.sample.foo']").value("bar"));
    }
}
