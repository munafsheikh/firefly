package ai.firefly.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class McpToolRegistry {

    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();

    public McpToolRegistry(ApplicationContext context) {
        context.getBeansOfType(McpTool.class).forEach((name, tool) -> {
            register(tool);
            log.info("Registered MCP tool: {}", tool.getToolName());
        });
    }

    public void register(McpTool tool) {
        tools.put(tool.getToolName(), tool);
    }

    public McpTool getTool(String toolName) {
        return tools.get(toolName);
    }

    public Collection<McpTool> getAllTools() {
        return tools.values();
    }

    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}
