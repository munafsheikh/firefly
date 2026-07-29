package ai.firefly.plugin.plantuml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/plantuml")
@RequiredArgsConstructor
public class PlantumlPluginController {

    private final PlantumlRenderService renderService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "plugin", "plantuml",
                "status", "ok"
        ));
    }

    @PostMapping(value = "/render", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> render(
            @RequestParam(value = "format", required = false) String format,
            @RequestBody(required = false) String source) {

        PlantumlFormat plantumlFormat;
        try {
            plantumlFormat = PlantumlFormat.fromParam(format);
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Unsupported format '" + format + "' — use 'svg' or 'png'");
        }

        try {
            byte[] image = renderService.render(source, plantumlFormat);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(plantumlFormat.contentType()))
                    .body(image);
        } catch (PlantumlRenderException e) {
            HttpStatus status = e.isClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
            return errorResponse(status, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error rendering PlantUML diagram", e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error rendering diagram");
        }
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", message,
                "status", status.value()
        ));
    }
}
