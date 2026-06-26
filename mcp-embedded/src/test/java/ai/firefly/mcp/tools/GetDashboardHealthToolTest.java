package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.dashboard.PluginInfo;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GetDashboardHealthToolTest {

    @Mock
    private DashboardService mockDashboardService;

    private GetDashboardHealthTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new GetDashboardHealthTool(mockDashboardService);
        // Inject server port via reflection
        ReflectionTestUtils.setField(tool, "serverPort", 17922);
    }

    @Test
    void testInvokeReturnsHealthStatus() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertEquals("healthy", health.get("status"));
    }

    @Test
    void testInvokeIncludesVersion() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertEquals("1.0.0", health.get("version"));
    }

    @Test
    void testInvokeIncludesPort() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertEquals(17922, health.get("port"));
    }

    @Test
    void testInvokeIncludesPluginCount() throws Exception {
        List<PluginInfo> plugins = List.of(
            new PluginInfo("id1", "Plugin 1", "1.0.0", "Desc 1", "Author 1"),
            new PluginInfo("id2", "Plugin 2", "2.0.0", "Desc 2", "Author 2")
        );
        when(mockDashboardService.getInstalledPlugins()).thenReturn(plugins);
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertEquals(2, health.get("pluginCount"));
    }

    @Test
    void testInvokeIncludesActuatorAvailability() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertTrue((Boolean) health.get("actuatorAvailable"));
    }

    @Test
    void testInvokeIncludesTimestamp() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertNotNull(health.get("timestamp"));
        assertTrue((Long) health.get("timestamp") > 0);
    }

    @Test
    void testInvokeWithZeroPlugins() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        when(mockDashboardService.isActuatorAvailable()).thenReturn(false);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertEquals(0, health.get("pluginCount"));
        assertFalse((Boolean) health.get("actuatorAvailable"));
    }

    @Test
    void testInvokeWithMultiplePlugins() throws Exception {
        List<PluginInfo> plugins = List.of(
            new PluginInfo("id1", "Plugin 1", "1.0.0", "Desc 1", "Author 1"),
            new PluginInfo("id2", "Plugin 2", "2.0.0", "Desc 2", "Author 2"),
            new PluginInfo("id3", "Plugin 3", "3.0.0", "Desc 3", "Author 3"),
            new PluginInfo("id4", "Plugin 4", "4.0.0", "Desc 4", "Author 4"),
            new PluginInfo("id5", "Plugin 5", "5.0.0", "Desc 5", "Author 5")
        );
        when(mockDashboardService.getInstalledPlugins()).thenReturn(plugins);
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result;
        assertEquals(5, health.get("pluginCount"));
    }
}
