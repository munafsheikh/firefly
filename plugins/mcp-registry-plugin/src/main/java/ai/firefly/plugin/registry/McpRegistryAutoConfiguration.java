package ai.firefly.plugin.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(McpRegistryProperties.class)
@ConditionalOnProperty(prefix = "firefly.plugin.mcp-registry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpRegistryAutoConfiguration {

    public McpRegistryAutoConfiguration() {
        log.info("Firefly MCP Registry Plugin auto-configuration loaded.");
    }

    @Bean
    public McpRegistryStore mcpRegistryStore(McpRegistryProperties props) {
        return new JsonMcpRegistryStore(props);
    }

    @Bean
    public McpRegistryService mcpRegistryService(McpRegistryStore store) {
        return new McpRegistryService(store);
    }

    @Bean
    public McpRegistryController mcpRegistryController(McpRegistryService service) {
        return new McpRegistryController(service);
    }

    @Bean
    public McpRegistryPageController mcpRegistryPageController(McpRegistryService service) {
        return new McpRegistryPageController(service);
    }

    @Bean
    public McpPluginTreeScanner mcpPluginTreeScanner() {
        return new McpPluginTreeScanner();
    }

    @Bean
    public McpPluginConfigResolver mcpPluginConfigResolver(ConfigurableEnvironment environment, McpPluginTreeScanner scanner) {
        return new McpPluginConfigResolver(environment, scanner);
    }

    @Bean
    public McpTreeController mcpTreeController(McpPluginTreeScanner scanner, McpPluginConfigResolver configResolver) {
        return new McpTreeController(scanner, configResolver);
    }
}
