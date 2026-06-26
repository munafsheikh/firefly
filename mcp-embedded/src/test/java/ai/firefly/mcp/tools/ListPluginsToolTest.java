package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.dashboard.PluginInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ListPluginsToolTest {

    @Mock
    private DashboardService mockDashboardService;

    private ListPluginsTool tool;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new ListPluginsTool(mockDashboardService);
    }

    @Test
    void testInvokeReturnsEmptyList() throws Exception {
        when(mockDashboardService.getInstalledPlugins()).thenReturn(new ArrayList<>());
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        assertNotNull(result);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertTrue(list.isEmpty());
    }

    @Test
    void testInvokeReturnsSinglePlugin() throws Exception {
        PluginInfo plugin = new PluginInfo("id1", "Plugin 1", "1.0", "desc", "author");
        List<PluginInfo> plugins = List.of(plugin);
        when(mockDashboardService.getInstalledPlugins()).thenReturn(plugins);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
    }

    @Test
    void testInvokeReturnsMultiplePlugins() throws Exception {
        List<PluginInfo> plugins = List.of(
            new PluginInfo("id1", "Plugin 1", "1.0", "desc1", "author1"),
            new PluginInfo("id2", "Plugin 2", "2.0", "desc2", "author2"),
            new PluginInfo("id3", "Plugin 3", "3.0", "desc3", "author3")
        );
        when(mockDashboardService.getInstalledPlugins()).thenReturn(plugins);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
    }

    @Test
    void testInvokePreservesPluginMetadata() throws Exception {
        PluginInfo plugin = new PluginInfo("test-id", "Test Plugin", "1.5.0", "Test Description", "Test Author");
        List<PluginInfo> plugins = List.of(plugin);
        when(mockDashboardService.getInstalledPlugins()).thenReturn(plugins);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        List<?> list = (List<?>) result;
        PluginInfo retrieved = (PluginInfo) list.get(0);
        assertEquals("test-id", retrieved.id());
        assertEquals("Test Plugin", retrieved.name());
        assertEquals("1.5.0", retrieved.version());
        assertEquals("Test Description", retrieved.description());
        assertEquals("Test Author", retrieved.author());
    }

    @Test
    void testInvokeJsonSerializable() throws Exception {
        List<PluginInfo> plugins = List.of(
            new PluginInfo("id1", "Plugin 1", "1.0", "desc1", "author1"),
            new PluginInfo("id2", "Plugin 2", "2.0", "desc2", "author2")
        );
        when(mockDashboardService.getInstalledPlugins()).thenReturn(plugins);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        // Verify the result can be serialized to JSON
        String json = mapper.writeValueAsString(result);
        assertNotNull(json);
        assertTrue(json.contains("id1"));
        assertTrue(json.contains("Plugin 1"));
    }
}
