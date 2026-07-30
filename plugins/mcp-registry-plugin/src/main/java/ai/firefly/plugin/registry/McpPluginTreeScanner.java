package ai.firefly.plugin.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Scans the plugins directory (same {@code firefly.plugins.path} location the core dashboard
 * scans for {@code META-INF/plugin.properties}) for plugin JARs that additionally bundle a
 * {@code META-INF/firefly/mcp-plugin.json} manifest declaring skills and/or MCP server
 * definitions, and groups them into one tree node per plugin.
 */
@Slf4j
@Component
public class McpPluginTreeScanner {

    private static final String MANIFEST_ENTRY = "META-INF/firefly/mcp-plugin.json";
    private static final String PLUGIN_PROPERTIES_ENTRY = "META-INF/plugin.properties";

    @Value("${firefly.plugins.path:plugins}")
    private String pluginsPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<McpPluginNode> scanTree() {
        List<McpPluginNode> nodes = new ArrayList<>();
        Path dir = Paths.get(pluginsPath);

        if (!Files.isDirectory(dir)) {
            log.debug("Plugins directory not found: {}", dir);
            return nodes;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path jarPath : stream) {
                McpPluginNode node = readNode(jarPath);
                if (node != null) {
                    nodes.add(node);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan plugins directory for MCP manifests: {}", dir, e);
        }

        nodes.sort(Comparator.comparing(McpPluginNode::pluginName, String.CASE_INSENSITIVE_ORDER));
        return nodes;
    }

    public Properties readPluginProperties(String pluginId) {
        Path dir = Paths.get(pluginsPath);
        if (!Files.isDirectory(dir)) {
            return new Properties();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path jarPath : stream) {
                Properties props = readPluginProperties(jarPath);
                if (pluginId.equals(props.getProperty("plugin.id"))) {
                    return props;
                }
            }
        } catch (IOException e) {
            log.warn("Failed to locate plugin.properties for {}", pluginId, e);
        }
        return new Properties();
    }

    private McpPluginNode readNode(Path jarPath) {
        Properties props = readPluginProperties(jarPath);
        if (props.isEmpty()) {
            return null;
        }

        McpPluginManifest manifest = readManifest(jarPath);
        if (manifest == null || manifest.isEmpty()) {
            return null;
        }

        return new McpPluginNode(
            props.getProperty("plugin.id", "unknown"),
            props.getProperty("plugin.name", "Unknown Plugin"),
            props.getProperty("plugin.version", "0.0.0"),
            manifest.skills(),
            manifest.servers()
        );
    }

    private Properties readPluginProperties(Path jarPath) {
        Properties props = new Properties();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry(PLUGIN_PROPERTIES_ENTRY);
            if (entry == null) {
                return props;
            }
            try (var is = jarFile.getInputStream(entry)) {
                props.load(is);
            }
        } catch (IOException e) {
            log.warn("Failed to read plugin.properties from {}", jarPath, e);
        }
        return props;
    }

    private McpPluginManifest readManifest(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry(MANIFEST_ENTRY);
            if (entry == null) {
                return null;
            }
            try (var is = jarFile.getInputStream(entry)) {
                return objectMapper.readValue(is, McpPluginManifest.class);
            }
        } catch (IOException e) {
            log.warn("Failed to read MCP plugin manifest from {}", jarPath, e);
            return null;
        }
    }
}
