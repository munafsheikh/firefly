package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServerTest {

    @Autowired
    private McpToolRegistry registry;

    @Autowired
    private McpProperties properties;

    @Autowired
    private McpServer server;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testMcpPropertiesLoaded() {
        assertTrue(properties.isEnabled());
        assertEquals("firefly-mcp", properties.getName());
        assertEquals("1.0.0", properties.getVersion());
    }

    @Test
    void testToolsRegistered() {
        assertTrue(registry.hasTool("firefly_list_plugins"));
        assertTrue(registry.hasTool("firefly_get_dashboard_health"));
        assertTrue(registry.hasTool("firefly_get_actuator_data"));
        assertTrue(registry.hasTool("firefly_list_actuator_endpoints"));
    }

    @Test
    void testListPluginsToolExists() {
        McpTool tool = registry.getTool("firefly_list_plugins");
        assertNotNull(tool);
        assertEquals("firefly_list_plugins", tool.getToolName());
    }

    @Test
    void testUnknownMethodReturnsErrorResponse() throws Exception {
        JsonNode request = mapper.createObjectNode()
            .put("jsonrpc", "2.0")
            .put("method", "nonexistent_method")
            .put("id", 1);

        JsonNode response = server.handleRequest(request);

        assertTrue(response.has("error"), "Response should contain error field");
        assertTrue(response.get("error").has("code"), "Error should have code");
        assertTrue(response.get("error").has("message"), "Error should have message");
        assertFalse(response.has("result"), "Error response should not have result");
    }

    @Test
    void testToolRegistryThreadSafety() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Register tools concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    McpTool tool = createMockTool("concurrent_tool_" + index, "Concurrent Tool " + index);
                    registry.register(tool);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Executor did not terminate in time");

        assertEquals(0, errorCount.get(), "No errors should occur during concurrent registration");
        assertEquals(threadCount, successCount.get(), "All registrations should succeed");

        // Verify all tools are registered
        long toolsCount = registry.getAllTools().stream()
            .filter(t -> t.getToolName().startsWith("concurrent_tool_"))
            .count();
        assertEquals(threadCount, toolsCount, "All concurrently registered tools should be in registry");
    }

    @SpringBootTest
    @TestPropertySource(properties = {"firefly.mcp.enabled=false"})
    static class DisabledMcpTest {
        @Autowired
        private McpProperties props;

        @Test
        void testMcpServerDisabledViaProperty() {
            assertFalse(props.isEnabled());
        }
    }

    private static McpTool createMockTool(String name, String description) {
        return new McpTool() {
            @Override
            public String getToolName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public java.util.Map<String, Object> getInputSchema() {
                return new java.util.HashMap<>();
            }

            @Override
            public Object invoke(JsonNode arguments) {
                return "result";
            }
        };
    }
}
