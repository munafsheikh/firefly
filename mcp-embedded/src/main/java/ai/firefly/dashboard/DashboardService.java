package ai.firefly.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

@Slf4j
@Service
public class DashboardService {

    @Value("${firefly.plugins.path:plugins}")
    private String pluginsPath;

    @Value("${server.port:8080}")
    private int serverPort;

    private volatile Boolean actuatorAvailable;

    public List<PluginInfo> getInstalledPlugins() {
        List<PluginInfo> plugins = new ArrayList<>();
        Path dir = Paths.get(pluginsPath);

        if (!Files.isDirectory(dir)) {
            log.debug("Plugins directory not found: {}", dir);
            return plugins;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path jarPath : stream) {
                PluginInfo info = readPluginMetadata(jarPath);
                if (info != null) {
                    plugins.add(info);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan plugins directory: {}", dir, e);
        }

        return plugins;
    }

    private PluginInfo readPluginMetadata(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry("META-INF/plugin.properties");
            if (entry == null) {
                String filename = jarPath.getFileName().toString();
                return new PluginInfo(
                    filename.replace(".jar", ""),
                    filename,
                    "unknown",
                    "No plugin metadata available",
                    ""
                );
            }
            Properties props = new Properties();
            try (var is = jarFile.getInputStream(entry)) {
                props.load(is);
            }
            return new PluginInfo(
                props.getProperty("plugin.id", "unknown"),
                props.getProperty("plugin.name", "Unknown Plugin"),
                props.getProperty("plugin.version", "0.0.0"),
                props.getProperty("plugin.description", ""),
                props.getProperty("plugin.author", "")
            );
        } catch (IOException e) {
            log.warn("Failed to read plugin metadata from {}", jarPath, e);
            return null;
        }
    }

    public boolean isActuatorAvailable() {
        if (actuatorAvailable == null) {
            try {
                Class.forName("org.springframework.boot.actuate.endpoint.annotation.Endpoint", false, getClass().getClassLoader());
                actuatorAvailable = true;
            } catch (ClassNotFoundException e) {
                actuatorAvailable = false;
            }
        }
        return actuatorAvailable;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getActuatorEndpoints() {
        if (!isActuatorAvailable()) {
            return null;
        }
        return Map.of("endpoints", List.of("health", "metrics", "info"));
    }
}
