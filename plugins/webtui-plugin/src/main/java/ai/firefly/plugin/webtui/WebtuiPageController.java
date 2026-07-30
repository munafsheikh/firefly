package ai.firefly.plugin.webtui;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebtuiPageController {

    private final WebtuiProperties properties;

    @GetMapping("/pages/webtui")
    public String webtuiPage(Model model) {
        model.addAttribute("homepage", properties.getHomepage());
        return "webtui-page";
    }
}
