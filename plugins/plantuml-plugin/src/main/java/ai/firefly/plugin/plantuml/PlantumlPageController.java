package ai.firefly.plugin.plantuml;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlantumlPageController {

    @GetMapping("/pages/plantuml")
    public String plantumlPage() {
        return "plantuml-page";
    }
}
