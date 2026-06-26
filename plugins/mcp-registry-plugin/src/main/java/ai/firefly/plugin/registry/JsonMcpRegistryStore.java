package ai.firefly.plugin.registry;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JsonMcpRegistryStore implements McpRegistryStore {

    private static final JavaType MCP_LIST_TYPE = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, McpRegistry.class);

    private final McpRegistryProperties properties;
    private final ObjectMapper objectMapper;
    private final Object lock = new Object();

    public JsonMcpRegistryStore(McpRegistryProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public McpRegistry register(McpRegistry mcp) {
        synchronized (lock) {
            List<McpRegistry> mcps = new ArrayList<>(loadMcps());
            int index = indexOf(mcps, mcp.id());
            if (index >= 0) {
                mcps.set(index, mcp);
            } else {
                mcps.add(mcp);
            }
            saveMcps(mcps);
            return mcp;
        }
    }

    @Override
    public void unregister(String id) {
        synchronized (lock) {
            List<McpRegistry> mcps = new ArrayList<>(loadMcps());
            mcps.removeIf(mcp -> mcp.id().equals(id));
            saveMcps(mcps);
        }
    }

    @Override
    public Optional<McpRegistry> getMcp(String id) {
        synchronized (lock) {
            return loadMcps().stream()
                .filter(mcp -> mcp.id().equals(id))
                .findFirst();
        }
    }

    @Override
    public List<McpRegistry> listAllMcps() {
        synchronized (lock) {
            return new ArrayList<>(loadMcps());
        }
    }

    @Override
    public McpRegistry updateMcp(McpRegistry mcp) {
        synchronized (lock) {
            List<McpRegistry> mcps = new ArrayList<>(loadMcps());
            int index = indexOf(mcps, mcp.id());
            if (index >= 0) {
                mcps.set(index, mcp);
            } else {
                mcps.add(mcp);
            }
            saveMcps(mcps);
            return mcp;
        }
    }

    private List<McpRegistry> loadMcps() {
        Path path = Path.of(properties.getStorePath());
        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            String content = Files.readString(path);
            if (content.isBlank()) {
                return List.of();
            }
            List<McpRegistry> mcps = objectMapper.readValue(content, MCP_LIST_TYPE);
            return mcps == null ? List.of() : mcps;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read MCP registry from " + path, e);
        }
    }

    private void saveMcps(List<McpRegistry> mcps) {
        Path path = Path.of(properties.getStorePath());
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path tempFile = parent == null
                ? Files.createTempFile("mcp-registry-", ".json")
                : Files.createTempFile(parent, "mcp-registry-", ".json");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), mcps);
                try {
                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write MCP registry to " + path, e);
        }
    }

    private int indexOf(List<McpRegistry> mcps, String id) {
        for (int i = 0; i < mcps.size(); i++) {
            if (mcps.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
