package ai.firefly.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        return "overview";
    }
}
