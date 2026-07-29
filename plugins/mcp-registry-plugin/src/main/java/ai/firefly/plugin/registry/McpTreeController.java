package ai.firefly.plugin.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the grouped MCP tree: plugin-contributed skills and MCP server definitions,
 * grouped by owning plugin, plus each plugin's resolved configuration for the tree's
 * "Settings" panel.
 */
@RestController
@RequestMapping("/api/mcp-registry")
@RequiredArgsConstructor
public class McpTreeController {

    private final McpPluginTreeScanner scanner;
    private final McpPluginConfigResolver configResolver;

    @GetMapping("/tree")
    public ResponseEntity<List<McpPluginNode>> tree() {
        return ResponseEntity.ok(scanner.scanTree());
    }

    @GetMapping("/tree/{pluginId}/config")
    public ResponseEntity<McpPluginConfig> config(@PathVariable String pluginId) {
        return ResponseEntity.ok(configResolver.resolve(pluginId));
    }
}
