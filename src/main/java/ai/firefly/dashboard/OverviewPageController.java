package ai.firefly.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OverviewPageController {

    private final DashboardService dashboardService;

    @GetMapping("/pages/overview")
    public String overviewPage(Model model) {
        model.addAttribute("actuatorAvailable", dashboardService.isActuatorAvailable());
        boolean adoInstalled = dashboardService.getInstalledPlugins().stream()
                .anyMatch(plugin -> "ado".equals(plugin.id()));
        boolean cliInstalled = dashboardService.getInstalledPlugins().stream()
                .anyMatch(plugin -> "cli".equals(plugin.id()));
        model.addAttribute("adoAvailable", adoInstalled);
        model.addAttribute("cliAvailable", cliInstalled);

        List<AlertMessage> alerts = new ArrayList<>();
        if (dashboardService.isActuatorAvailable()) {
            alerts.add(AlertMessage.success("Actuator endpoint detected"));
        }
        if (adoInstalled) {
            alerts.add(AlertMessage.info("ADO plugin is installed and ready"));
        }
        if (!cliInstalled) {
            alerts.add(AlertMessage.warning("CLI plugin not found — web terminal may be unavailable"));
        }
        model.addAttribute("alerts", alerts);

        return "overview";
    }
}
