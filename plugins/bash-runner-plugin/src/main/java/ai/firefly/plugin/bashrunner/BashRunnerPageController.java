package ai.firefly.plugin.bashrunner;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BashRunnerPageController {

    @GetMapping("/bash-runner")
    public String page() {
        return "bash-runner";
    }
}
