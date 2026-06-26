package ai.firefly.mcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firefly.mcp")
public class McpProperties {

    private boolean enabled = true;
    private String name = "firefly-mcp";
    private String version = "1.0.0";
    private String description = "Firefly Dashboard & Plugin Management via MCP";

    private int maxToolsDiscoveryTimeMs = 5000;
    private int toolInvocationTimeoutMs = 30000;
    private boolean debugLogging = false;
}
