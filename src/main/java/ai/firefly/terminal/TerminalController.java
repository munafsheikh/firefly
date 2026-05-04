package ai.firefly.terminal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TerminalController {

    private final TerminalProperties properties;

    public TerminalController(TerminalProperties properties) {
        this.properties = properties;
    }

    @GetMapping("${firefly.terminal.path:/terminal}")
    public String terminalPage() {
        return "forward:/terminal.html";
    }
}
