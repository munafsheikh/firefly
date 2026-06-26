package ai.firefly.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpPropertiesTest {

    @Autowired
    private McpProperties properties;

    @Test
    void testDefaultPropertiesValues() {
        assertTrue(properties.isEnabled());
        assertEquals("firefly-mcp", properties.getName());
        assertEquals("1.0.0", properties.getVersion());
        assertEquals("Firefly Dashboard & Plugin Management via MCP", properties.getDescription());
    }

    @Test
    void testDefaultTimeoutValues() {
        assertEquals(5000, properties.getMaxToolsDiscoveryTimeMs());
        assertEquals(30000, properties.getToolInvocationTimeoutMs());
    }

    @Test
    void testDebugLoggingDisabledByDefault() {
        assertFalse(properties.isDebugLogging());
    }

    @SpringBootTest
    @TestPropertySource(properties = {"firefly.mcp.enabled=false"})
    static class DisabledPropertyTest {
        @Autowired
        private McpProperties props;

        @Test
        void testMcpDisabledProperty() {
            assertFalse(props.isEnabled());
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {"firefly.mcp.enabled=true"})
    static class EnabledPropertyTest {
        @Autowired
        private McpProperties props;

        @Test
        void testMcpEnabledProperty() {
            assertTrue(props.isEnabled());
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {
        "firefly.mcp.name=custom-mcp",
        "firefly.mcp.version=2.0.0",
        "firefly.mcp.description=Custom MCP Description"
    })
    static class CustomPropertiesTest {
        @Autowired
        private McpProperties props;

        @Test
        void testCustomName() {
            assertEquals("custom-mcp", props.getName());
        }

        @Test
        void testCustomVersion() {
            assertEquals("2.0.0", props.getVersion());
        }

        @Test
        void testCustomDescription() {
            assertEquals("Custom MCP Description", props.getDescription());
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {
        "firefly.mcp.maxToolsDiscoveryTimeMs=10000",
        "firefly.mcp.toolInvocationTimeoutMs=60000"
    })
    static class CustomTimeoutsTest {
        @Autowired
        private McpProperties props;

        @Test
        void testCustomDiscoveryTimeout() {
            assertEquals(10000, props.getMaxToolsDiscoveryTimeMs());
        }

        @Test
        void testCustomInvocationTimeout() {
            assertEquals(60000, props.getToolInvocationTimeoutMs());
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {"firefly.mcp.debugLogging=true"})
    static class DebugLoggingTest {
        @Autowired
        private McpProperties props;

        @Test
        void testDebugLoggingEnabled() {
            assertTrue(props.isDebugLogging());
        }
    }
}
