package ai.firefly.plugin.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = McpRegistryIntegrationTest.TestApplication.class, properties = "firefly.plugin.mcp-registry.enabled=true")
class McpRegistryIntegrationTest {

    private static final Path STORE_DIR = createStoreDir();

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(McpRegistryAutoConfiguration.class)
    static class TestApplication {
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @org.springframework.test.context.DynamicPropertySource
    static void registerProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("firefly.plugin.mcp-registry.store-path", () -> STORE_DIR.resolve("registry.json").toString());
    }

    @Test
    void registerFetchAndDeleteMcp() throws Exception {
        McpRegistry mcp = new McpRegistry(
            "mcp-1",
            "Integration MCP",
            McpServerType.HTTP,
            "http://localhost:8081",
            Map.of("TOKEN", "secret"),
            List.of(),
            true,
            "1.0.0"
        );

        mockMvc.perform(post("/api/mcp-registry/mcps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mcp)))
            .andExpect(status().isCreated());

        assertThat(Files.exists(STORE_DIR.resolve("registry.json"))).isTrue();

        mockMvc.perform(get("/api/mcp-registry/mcps/mcp-1"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/mcp-registry/mcps/mcp-1"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/mcp-registry/mcps/mcp-1"))
            .andExpect(status().isNotFound());
    }

    private static Path createStoreDir() {
        try {
            return Files.createTempDirectory("mcp-registry-integration-");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create temp directory", e);
        }
    }
}
