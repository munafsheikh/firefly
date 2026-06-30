package ai.firefly.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CliPageController {

    private final DashboardService dashboardService;

    @GetMapping("/pages/cli")
    public String cliPage(Model model) {
        boolean cliPluginInstalled = dashboardService.getInstalledPlugins().stream()
                .anyMatch(plugin -> "cli".equals(plugin.id()));
        model.addAttribute("cliPluginInstalled", cliPluginInstalled);

        if (cliPluginInstalled) {
            model.addAttribute("alerts", List.of(
                    AlertMessage.success("CLI plugin is installed and ready"),
                    AlertMessage.info("Open the web terminal to launch the TUI")
            ));
        } else {
            model.addAttribute("alerts", List.of(
                    AlertMessage.warning("CLI plugin not found — drop cli-plugin*.jar into plugins/")
            ));
        }

        return "cli-page";
    }
}
