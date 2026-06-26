package ai.firefly.plugin.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mcp-registry")
@RequiredArgsConstructor
@Slf4j
public class McpRegistryPageController {

    private final McpRegistryService service;

    @GetMapping({"", "/"})
    public String registry(Model model) {
        model.addAttribute("mcps", service.listAllMcps());
        return "registry";
    }
}
