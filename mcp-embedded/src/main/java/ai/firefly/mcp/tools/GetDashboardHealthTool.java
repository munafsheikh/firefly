package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GetDashboardHealthTool implements McpTool {

    private final DashboardService dashboardService;

    @Value("${server.port:17922}")
    private int serverPort;

    public GetDashboardHealthTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public String getToolName() {
        return "firefly_get_dashboard_health";
    }

    @Override
    public String getDescription() {
        return "Get overall health status of Firefly dashboard including plugin availability";
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
    public Object invoke(JsonNode arguments) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("version", "1.0.0");
        health.put("port", serverPort);
        health.put("pluginCount", dashboardService.getInstalledPlugins().size());
        health.put("actuatorAvailable", dashboardService.isActuatorAvailable());
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
