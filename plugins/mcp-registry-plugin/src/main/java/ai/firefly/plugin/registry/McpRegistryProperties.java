package ai.firefly.plugin.registry;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firefly.plugin.mcp-registry")
public class McpRegistryProperties {

    private boolean enabled = true;
    private String storePath = System.getProperty("user.home") + "/.firefly/registry.json";
}
