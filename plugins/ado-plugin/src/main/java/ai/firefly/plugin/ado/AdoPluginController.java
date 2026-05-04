package ai.firefly.plugin.ado;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ado")
@RequiredArgsConstructor
public class AdoPluginController {

    private final AdoWorkItemService workItemService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean connected = workItemService.isConnected();
        return ResponseEntity.ok(Map.of(
            "status", connected ? "connected" : "disconnected",
            "plugin", "ado",
            "connected", connected
        ));
    }

    @GetMapping("/workitems/{id}")
    public ResponseEntity<Map<String, Object>> getWorkItem(@PathVariable int id) {
        return ResponseEntity.ok(workItemService.getWorkItem(id));
    }

    @PostMapping("/workitems/query")
    public ResponseEntity<List<Map<String, Object>>> queryWorkItems(@RequestBody Map<String, String> body) {
        String wiql = body.getOrDefault("query", "SELECT [System.Id] FROM workitems");
        return ResponseEntity.ok(workItemService.getWorkItems(wiql));
    }

    @GetMapping("/workitems/open-bugs")
    public ResponseEntity<List<Map<String, Object>>> getOpenBugs() {
        return ResponseEntity.ok(workItemService.getOpenBugs());
    }

    @GetMapping("/workitems/tasks")
    public ResponseEntity<List<Map<String, Object>>> getTasksForUser(@RequestParam String user) {
        return ResponseEntity.ok(workItemService.getTasksForUser(user));
    }

    @PatchMapping("/workitems/{id}/field")
    public ResponseEntity<Map<String, Object>> updateField(
            @PathVariable int id,
            @RequestBody Map<String, Object> body) {
        String field = (String) body.get("field");
        Object value = body.get("value");
        return ResponseEntity.ok(workItemService.updateWorkItemField(id, field, value));
    }

    @PatchMapping("/workitems/{id}/fields")
    public ResponseEntity<Map<String, Object>> updateFields(
            @PathVariable int id,
            @RequestBody Map<String, Object> fields) {
        return ResponseEntity.ok(workItemService.updateWorkItemFields(id, fields));
    }

    @PatchMapping("/workitems/{id}/state")
    public ResponseEntity<Map<String, Object>> updateState(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(workItemService.updateWorkItemState(id, body.get("state")));
    }

    @PatchMapping("/workitems/{id}/assign")
    public ResponseEntity<Map<String, Object>> assign(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(workItemService.assignWorkItem(id, body.get("user")));
    }

    @PostMapping("/workitems")
    public ResponseEntity<Map<String, Object>> createWorkItem(@RequestBody CreateWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.createWorkItem(request.type(), request.title(), request.description()));
    }

    @PostMapping("/tasks")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody CreateWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.createTask(request.title(), request.description()));
    }

    @PostMapping("/bugs")
    public ResponseEntity<Map<String, Object>> createBug(@RequestBody CreateWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.createBug(request.title(), request.description()));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getProjects() {
        return ResponseEntity.ok(workItemService.getProjects());
    }

    public record CreateWorkItemRequest(String type, String title, String description) {}
}
