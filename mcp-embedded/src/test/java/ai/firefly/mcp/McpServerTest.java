package ai.firefly.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServerTest {

    @Autowired
    private McpToolRegistry registry;

    @Autowired
    private McpProperties properties;

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
}
