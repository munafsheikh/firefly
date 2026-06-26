package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Manages MCP server lifecycle: start when Spring context ready, stop on shutdown.
 */
@Slf4j
@Component
public class McpServerLifecycle {

    private final McpServer server;
    private final McpProperties properties;

    public McpServerLifecycle(McpServer server, McpProperties properties) {
        this.server = server;
        this.properties = properties;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (properties.isEnabled()) {
            server.start();
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        server.stop();
    }
}
