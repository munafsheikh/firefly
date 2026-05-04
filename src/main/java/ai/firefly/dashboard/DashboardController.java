package ai.firefly.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("appName", "Firefly");
        model.addAttribute("appVersion", "0.0.1-SNAPSHOT");
        model.addAttribute("plugins", dashboardService.getInstalledPlugins());
        model.addAttribute("actuatorAvailable", dashboardService.isActuatorAvailable());

        Map<String, Object> actuatorResponse = dashboardService.getActuatorEndpoints();
        Map<String, Map<String, String>> actuatorLinks = Map.of();
        if (actuatorResponse.containsKey("_links")) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> links = (Map<String, Map<String, String>>) actuatorResponse.get("_links");
            actuatorLinks = links;
        }
        model.addAttribute("actuatorLinks", actuatorLinks);
        model.addAttribute("swaggerAvailable", true);
        model.addAttribute("terminalAvailable", true);

        return "dashboard";
    }
}
