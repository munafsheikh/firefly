package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ListActuatorEndpointsTool implements McpTool {

    private final DashboardService dashboardService;

    public ListActuatorEndpointsTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public String getToolName() {
        return "firefly_list_actuator_endpoints";
    }

    @Override
    public String getDescription() {
        return "List available Spring Boot actuator endpoints (health, metrics, info, etc)";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", new String[]{}
        );
    }

    @Override
    public Object invoke(JsonNode arguments) throws Exception {
        if (!dashboardService.isActuatorAvailable()) {
            Map<String, String> result = new HashMap<>();
            result.put("available", "false");
            result.put("message", "Actuator plugin not loaded");
            return result;
        }

        Map<String, Object> endpoints = dashboardService.getActuatorEndpoints();
        return endpoints != null ? endpoints : new HashMap<>();
    }
}
