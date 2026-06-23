package ai.firefly.plugin.ado;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdoPageController {

    @GetMapping("/pages/ado")
    public String adoPage() {
        return "ado-page";
    }
}
