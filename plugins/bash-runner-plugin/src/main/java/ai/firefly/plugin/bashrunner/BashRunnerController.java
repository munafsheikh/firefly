package ai.firefly.plugin.bashrunner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bash-runner")
public class BashRunnerController {

    private final BashRunnerService service;
    private final BashRunnerProperties properties;
    private final XdgOpenService xdgOpenService;

    public BashRunnerController(BashRunnerService service, BashRunnerProperties properties, XdgOpenService xdgOpenService) {
        this.service = service;
        this.properties = properties;
        this.xdgOpenService = xdgOpenService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() throws IOException {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("plugin", "bash-runner");
        health.put("status", "UP");
        health.put("scriptsRoot", service.getScriptsRoot().toString());
        health.put("bashCommand", properties.getBashCommand());
        health.put("xdgOpenEnabled", properties.isXdgOpenEnabled());
        health.put("xdgOpenAvailable", xdgOpenService.isAvailable());
        return health;
    }

    @GetMapping("/scripts")
    public List<ScriptInfo> scripts() throws IOException {
        return service.listScripts();
    }

    @PostMapping("/run")
    public BashRunResult run(@RequestBody BashRunRequest request) throws IOException, InterruptedException {
        return service.run(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> ioFailure(IOException e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<Map<String, Object>> interrupted(InterruptedException e) {
        Thread.currentThread().interrupt();
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "script execution was interrupted");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message));
    }
}
