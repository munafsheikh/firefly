package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ListActuatorEndpointsToolTest {

    @Mock
    private DashboardService mockDashboardService;

    private ListActuatorEndpointsTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new ListActuatorEndpointsTool(mockDashboardService);
    }

    @Test
    void testInvokeWhenActuatorAvailable() throws Exception {
        Map<String, Object> endpoints = Map.of(
            "endpoints", List.of("health", "metrics", "info")
        );
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        when(mockDashboardService.getActuatorEndpoints()).thenReturn(endpoints);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertTrue(resultMap.containsKey("endpoints"));
    }

    @Test
    void testInvokeWhenActuatorUnavailable() throws Exception {
        when(mockDashboardService.isActuatorAvailable()).thenReturn(false);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("false", resultMap.get("available"));
        assertTrue(resultMap.get("message").toString().contains("Actuator"));
    }

    @Test
    void testInvokeWhenActuatorReturnsNull() throws Exception {
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        when(mockDashboardService.getActuatorEndpoints()).thenReturn(null);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertTrue(resultMap.isEmpty());
    }

    @Test
    void testInvokeReturnsConsistentStructure() throws Exception {
        Map<String, Object> endpoints = new HashMap<>();
        when(mockDashboardService.isActuatorAvailable()).thenReturn(true);
        when(mockDashboardService.getActuatorEndpoints()).thenReturn(endpoints);
        
        Object result = tool.invoke(MissingNode.getInstance());
        
        assertNotNull(result);
        assertTrue(result instanceof Map, "Result should always be a Map");
    }

    @Test
    void testToolNameIsCorrect() {
        assertEquals("firefly_list_actuator_endpoints", tool.getToolName());
    }

    @Test
    void testDescriptionIsPresent() {
        String description = tool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("actuator") || description.contains("Actuator"));
    }

    @Test
    void testInputSchemaIsEmpty() {
        var schema = tool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("properties"));
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties = (java.util.Map<String, Object>) schema.get("properties");
        assertTrue(properties.isEmpty());
    }
}
