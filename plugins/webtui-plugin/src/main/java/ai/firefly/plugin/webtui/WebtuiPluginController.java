package ai.firefly.plugin.webtui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API for the WebTUI Browser plugin: drives the single shared headless-Chromium page
 * ({@link BrowserSessionService}) and serves its rendered output as a screenshot.
 *
 * <p>Every endpoint degrades cleanly (503 + JSON body) instead of a 500 stack trace when no working
 * browser is available in this environment.
 */
@Slf4j
@RestController
@RequestMapping("/api/webtui")
@RequiredArgsConstructor
public class WebtuiPluginController {

    private final BrowserSessionService browserSessionService;

    public record NavigateRequest(String url) {
    }

    public record ClickRequest(double x, double y) {
    }

    public record ScrollRequest(double deltaY) {
    }

    public record TypeRequest(String text) {
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plugin", "webtui");
        body.put("status", "ok");
        body.put("browserAvailable", browserSessionService.isAvailable());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/navigate")
    public ResponseEntity<Map<String, Object>> navigate(@RequestBody NavigateRequest request) {
        if (!browserSessionService.isAvailable()) {
            return unavailable();
        }
        if (request == null || request.url() == null || request.url().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required field: url"));
        }
        try {
            BrowserSessionService.PageInfo info = browserSessionService.navigate(request.url());
            return ResponseEntity.ok(Map.of("url", info.url(), "title", info.title()));
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return unavailable();
        } catch (Exception e) {
            log.warn("WebTUI plugin: navigation to {} failed: {}", request.url(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Navigation failed: " + safeMessage(e)));
        }
    }

    @GetMapping(value = "/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> screenshot() {
        if (!browserSessionService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJsonBytes(browserSessionService.getUnavailableReason()));
        }
        try {
            byte[] png = browserSessionService.screenshot();
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJsonBytes(e.getMessage()));
        } catch (Exception e) {
            log.warn("WebTUI plugin: screenshot failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJsonBytes("Screenshot failed: " + safeMessage(e)));
        }
    }

    @PostMapping("/click")
    public ResponseEntity<Map<String, Object>> click(@RequestBody ClickRequest request) {
        if (!browserSessionService.isAvailable()) {
            return unavailable();
        }
        try {
            browserSessionService.click(request.x(), request.y());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return unavailable();
        } catch (Exception e) {
            log.warn("WebTUI plugin: click failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", safeMessage(e)));
        }
    }

    @PostMapping("/scroll")
    public ResponseEntity<Map<String, Object>> scroll(@RequestBody ScrollRequest request) {
        if (!browserSessionService.isAvailable()) {
            return unavailable();
        }
        try {
            browserSessionService.scroll(request.deltaY());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return unavailable();
        } catch (Exception e) {
            log.warn("WebTUI plugin: scroll failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", safeMessage(e)));
        }
    }

    @PostMapping("/type")
    public ResponseEntity<Map<String, Object>> type(@RequestBody TypeRequest request) {
        if (!browserSessionService.isAvailable()) {
            return unavailable();
        }
        if (request == null || request.text() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required field: text"));
        }
        try {
            browserSessionService.type(request.text());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return unavailable();
        } catch (Exception e) {
            log.warn("WebTUI plugin: type failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", safeMessage(e)));
        }
    }

    @PostMapping("/back")
    public ResponseEntity<Map<String, Object>> back() {
        if (!browserSessionService.isAvailable()) {
            return unavailable();
        }
        try {
            BrowserSessionService.PageInfo info = browserSessionService.goBack();
            return ResponseEntity.ok(Map.of("url", info.url(), "title", info.title()));
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return unavailable();
        } catch (Exception e) {
            log.warn("WebTUI plugin: back failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", safeMessage(e)));
        }
    }

    @PostMapping("/forward")
    public ResponseEntity<Map<String, Object>> forward() {
        if (!browserSessionService.isAvailable()) {
            return unavailable();
        }
        try {
            BrowserSessionService.PageInfo info = browserSessionService.goForward();
            return ResponseEntity.ok(Map.of("url", info.url(), "title", info.title()));
        } catch (BrowserSessionService.BrowserUnavailableException e) {
            return unavailable();
        } catch (Exception e) {
            log.warn("WebTUI plugin: forward failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", safeMessage(e)));
        }
    }

    private ResponseEntity<Map<String, Object>> unavailable() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plugin", "webtui");
        body.put("status", "unavailable");
        body.put("browserAvailable", false);
        body.put("error", browserSessionService.getUnavailableReason());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private byte[] errorJsonBytes(String message) {
        String safe = message == null ? "" : message.replace("\"", "'");
        String json = "{\"plugin\":\"webtui\",\"status\":\"unavailable\",\"browserAvailable\":false,\"error\":\""
                + safe + "\"}";
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
