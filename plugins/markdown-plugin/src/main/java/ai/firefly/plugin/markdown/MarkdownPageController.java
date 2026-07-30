package ai.firefly.plugin.markdown;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MarkdownPageController {

    @GetMapping("/pages/markdown")
    public String markdownPage() {
        return "markdown-page";
    }
}
