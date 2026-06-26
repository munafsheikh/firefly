package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "firefly.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public McpToolRegistry mcpToolRegistry(org.springframework.context.ApplicationContext context) {
        return new McpToolRegistry(context);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServer mcpServer(McpToolRegistry registry, McpProperties properties) {
        return new McpServer(registry, properties);
    }

    @Bean
    public McpServerLifecycle mcpServerLifecycle(McpServer server, McpProperties properties) {
        return new McpServerLifecycle(server, properties);
    }
}
