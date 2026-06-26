package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ListPluginsTool implements McpTool {

    private final DashboardService dashboardService;

    public ListPluginsTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public String getToolName() {
        return "firefly_list_plugins";
    }

    @Override
    public String getDescription() {
        return "List all installed Firefly plugins with metadata (id, name, version, author)";
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
        return dashboardService.getInstalledPlugins();
    }
}
