package ai.firefly.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CliPageController {

    private final DashboardService dashboardService;

    @GetMapping("/pages/cli")
    public String cliPage(Model model) {
        boolean cliPluginInstalled = dashboardService.getInstalledPlugins().stream()
                .anyMatch(plugin -> "cli".equals(plugin.id()));
        model.addAttribute("cliPluginInstalled", cliPluginInstalled);
        return "cli-page";
    }
}
