package ai.firefly.theme;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/theme")
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeManager themeManager;

    @GetMapping
    public ResponseEntity<Map<String, Object>> state() {
        List<ThemeInfo> themes = themeManager.listThemes();
        return ResponseEntity.ok(Map.of(
            "activeThemeId", themeManager.getActiveThemeId(),
            "themes", themes
        ));
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> activate(@PathVariable String id) {
        try {
            themeManager.setActiveTheme(id);
            return ResponseEntity.ok(Map.of("activeThemeId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/active.css", produces = "text/css")
    public String activeCss() {
        return themeManager.getActiveThemeCss();
    }
}
