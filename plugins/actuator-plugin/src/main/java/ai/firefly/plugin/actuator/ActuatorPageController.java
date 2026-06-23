package ai.firefly.plugin.actuator;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActuatorPageController {

    @GetMapping("/pages/actuator")
    public String actuatorPage() {
        return "actuator-page";
    }
}
