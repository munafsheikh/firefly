package ai.firefly.mcp.tools;

import ai.firefly.dashboard.DashboardService;
import ai.firefly.mcp.McpTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class GetActuatorDataTool implements McpTool {

    private final DashboardService dashboardService;
    private final RestClient restClient = RestClient.create();

    public GetActuatorDataTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public String getToolName() {
        return "firefly_get_actuator_data";
    }

    @Override
    public String getDescription() {
        return "Fetch data from a Spring Boot actuator endpoint (e.g., health, metrics, info)";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "endpoint", Map.of(
                    "type", "string",
                    "description", "Actuator endpoint name (e.g., 'health', 'metrics', 'env')"
                )
            ),
            "required", new String[]{"endpoint"}
        );
    }

    @Override
    public Object invoke(JsonNode arguments) throws Exception {
        if (!dashboardService.isActuatorAvailable()) {
            throw new IllegalStateException("Actuator not available");
        }

        String endpoint = arguments.path("endpoint").asText();
        String url = "http://localhost:17922/actuator/" + endpoint;

        try {
            return restClient.get()
                .uri(url)
                .retrieve()
                .body(Object.class);
        } catch (Exception e) {
            throw new Exception("Failed to fetch actuator endpoint: " + endpoint, e);
        }
    }
}
