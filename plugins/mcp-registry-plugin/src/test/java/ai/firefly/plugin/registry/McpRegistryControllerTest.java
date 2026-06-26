package ai.firefly.plugin.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class McpRegistryControllerTest {

    @Mock
    private McpRegistryService service;

    @InjectMocks
    private McpRegistryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void listMcpsReturnsOk() throws Exception {
        when(service.listAllMcps()).thenReturn(List.of(sampleMcp()));

        mockMvc.perform(get("/api/mcp-registry/mcps"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value("mcp-1"));
    }

    @Test
    void createMcpReturnsCreated() throws Exception {
        McpRegistry mcp = sampleMcp();
        when(service.register(any(McpRegistry.class))).thenReturn(mcp);

        mockMvc.perform(post("/api/mcp-registry/mcps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(mcp)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("mcp-1"));
    }

    @Test
    void getMcpReturnsOkWhenFound() throws Exception {
        when(service.getMcp("mcp-1")).thenReturn(Optional.of(sampleMcp()));

        mockMvc.perform(get("/api/mcp-registry/mcps/mcp-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("mcp-1"));
    }

    @Test
    void getMcpReturnsNotFoundWhenMissing() throws Exception {
        when(service.getMcp("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mcp-registry/mcps/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteMcpReturnsNoContent() throws Exception {
        doNothing().when(service).unregister("mcp-1");

        mockMvc.perform(delete("/api/mcp-registry/mcps/mcp-1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void refreshMcpReturnsOk() throws Exception {
        when(service.refresh("mcp-1")).thenReturn(sampleMcp());

        mockMvc.perform(post("/api/mcp-registry/mcps/mcp-1/refresh"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("mcp-1"));
    }

    private McpRegistry sampleMcp() {
        return new McpRegistry(
            "mcp-1",
            "Sample MCP",
            McpServerType.HTTP,
            "http://localhost:3000",
            Map.of("TOKEN", "secret"),
            List.of(),
            true,
            "1.0.0"
        );
    }
}
