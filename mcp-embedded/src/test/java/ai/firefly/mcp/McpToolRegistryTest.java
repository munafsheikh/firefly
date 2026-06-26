package ai.firefly.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class McpToolRegistryTest {

    private McpToolRegistry registry;
    private ApplicationContext mockContext;

    @BeforeEach
    void setUp() {
        mockContext = mock(ApplicationContext.class);
        Mockito.when(mockContext.getBeansOfType(McpTool.class))
            .thenReturn(new HashMap<>());
        registry = new McpToolRegistry(mockContext);
    }

    @Test
    void testRegisterTool() {
        McpTool tool = createMockTool("test_tool", "Test Tool");
        registry.register(tool);
        
        assertTrue(registry.hasTool("test_tool"));
        assertNotNull(registry.getTool("test_tool"));
    }

    @Test
    void testGetToolReturnsRegisteredTool() {
        McpTool tool = createMockTool("my_tool", "My Tool");
        registry.register(tool);
        
        McpTool retrieved = registry.getTool("my_tool");
        assertNotNull(retrieved);
        assertEquals("my_tool", retrieved.getToolName());
    }

    @Test
    void testGetToolReturnsNullForMissing() {
        McpTool result = registry.getTool("nonexistent");
        assertNull(result);
    }

    @Test
    void testGetAllToolsReturnsCollection() {
        McpTool tool1 = createMockTool("tool1", "Tool 1");
        McpTool tool2 = createMockTool("tool2", "Tool 2");
        registry.register(tool1);
        registry.register(tool2);
        
        Collection<McpTool> tools = registry.getAllTools();
        assertNotNull(tools);
        assertEquals(2, tools.size());
        assertTrue(tools.stream().anyMatch(t -> t.getToolName().equals("tool1")));
        assertTrue(tools.stream().anyMatch(t -> t.getToolName().equals("tool2")));
    }

    @Test
    void testHasToolReturnsTrueForExisting() {
        McpTool tool = createMockTool("existing", "Existing Tool");
        registry.register(tool);
        
        assertTrue(registry.hasTool("existing"));
    }

    @Test
    void testHasToolReturnsFalseForMissing() {
        assertFalse(registry.hasTool("nonexistent"));
    }

    @Test
    void testConcurrentRegisterAndLookup() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        // Register tools concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    McpTool tool = createMockTool("tool_" + index, "Tool " + index);
                    registry.register(tool);
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        // Lookup tools concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    McpTool tool = registry.getTool("tool_" + index);
                    if (tool == null) {
                        errors.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Executor did not terminate in time");
        
        assertEquals(0, errors.get(), "Concurrent operations resulted in errors");
        assertEquals(threadCount, registry.getAllTools().size(), "Registry should have all registered tools");
    }

    private McpTool createMockTool(String name, String description) {
        McpTool tool = mock(McpTool.class);
        Mockito.when(tool.getToolName()).thenReturn(name);
        Mockito.when(tool.getDescription()).thenReturn(description);
        Mockito.when(tool.getInputSchema()).thenReturn(new HashMap<>());
        return tool;
    }
}
