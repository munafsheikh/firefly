package ai.firefly.plugin.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/mcp-registry")
@RequiredArgsConstructor
public class McpRegistryController {

    private final McpRegistryService service;

    @GetMapping("/mcps")
    public ResponseEntity<List<McpRegistry>> listMcps() {
        return ResponseEntity.ok(service.listAllMcps());
    }

    @PostMapping("/mcps")
    public ResponseEntity<McpRegistry> createMcp(@RequestBody McpRegistry mcp) {
        McpRegistry saved = service.register(mcp);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/mcps/{id}")
    public ResponseEntity<McpRegistry> getMcpById(@PathVariable String id) {
        Optional<McpRegistry> mcp = service.getMcp(id);
        return mcp.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/mcps/{id}")
    public ResponseEntity<Void> deleteMcp(@PathVariable String id) {
        service.unregister(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mcps/{id}/refresh")
    public ResponseEntity<McpRegistry> refreshMcp(@PathVariable String id) {
        try {
            McpRegistry updated = service.refresh(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
