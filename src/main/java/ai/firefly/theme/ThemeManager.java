package ai.firefly.theme;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Discovers theme plugins (JARs under the plugins directory bundling
 * {@code META-INF/firefly/theme.css}) the same way {@code DashboardService} discovers
 * regular plugins, and tracks which theme (built-in default, or a theme plugin) is active.
 * The active theme's CSS is served at {@code /api/theme/active.css} and linked from the
 * dashboard so switching themes takes effect without any template rebuild.
 */
@Slf4j
@Service
public class ThemeManager {

    private static final String PLUGIN_PROPERTIES_ENTRY = "META-INF/plugin.properties";
    private static final String THEME_CSS_ENTRY = "META-INF/firefly/theme.css";

    @Value("${firefly.plugins.path:plugins}")
    private String pluginsPath;

    @Value("${firefly.theme.state-path:${user.home}/.firefly/theme.state}")
    private String statePath;

    public List<ThemeInfo> listThemes() {
        List<ThemeInfo> themes = new ArrayList<>();
        themes.add(ThemeInfo.builtInDefault());

        Path dir = Paths.get(pluginsPath);
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
                for (Path jarPath : stream) {
                    ThemeInfo info = readThemeInfo(jarPath);
                    if (info != null) {
                        themes.add(info);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to scan plugins directory for themes: {}", dir, e);
            }
        }

        themes.sort(Comparator.comparing((ThemeInfo t) -> !t.builtIn()).thenComparing(ThemeInfo::name, String.CASE_INSENSITIVE_ORDER));
        return themes;
    }

    public String getActiveThemeId() {
        String stored = readState();
        if (stored == null) {
            return ThemeInfo.DEFAULT_THEME_ID;
        }
        boolean exists = listThemes().stream().anyMatch(t -> t.id().equals(stored));
        return exists ? stored : ThemeInfo.DEFAULT_THEME_ID;
    }

    public void setActiveTheme(String id) {
        boolean exists = listThemes().stream().anyMatch(t -> t.id().equals(id));
        if (!exists) {
            throw new IllegalArgumentException("Unknown theme: " + id);
        }
        writeState(id);
    }

    /**
     * CSS for the currently active theme, meant to override the {@code :root} custom
     * properties defined in the dashboard's base stylesheet. Empty for the built-in default
     * theme (no override needed) or if the active theme's CSS can no longer be located.
     */
    public String getActiveThemeCss() {
        String id = getActiveThemeId();
        if (ThemeInfo.DEFAULT_THEME_ID.equals(id)) {
            return "";
        }

        Path dir = Paths.get(pluginsPath);
        if (!Files.isDirectory(dir)) {
            return "";
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path jarPath : stream) {
                Properties props = readPluginProperties(jarPath);
                if (id.equals(props.getProperty("plugin.id"))) {
                    String css = readThemeCss(jarPath);
                    if (css != null) {
                        return css;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load active theme CSS for {}", id, e);
        }
        return "";
    }

    private ThemeInfo readThemeInfo(Path jarPath) {
        Properties props = readPluginProperties(jarPath);
        if (props.isEmpty()) {
            return null;
        }
        String css = readThemeCss(jarPath);
        if (css == null) {
            return null;
        }
        return new ThemeInfo(
            props.getProperty("plugin.id", "unknown"),
            props.getProperty("plugin.name", "Unknown Theme"),
            props.getProperty("plugin.version", "0.0.0"),
            props.getProperty("plugin.description", ""),
            props.getProperty("plugin.author", ""),
            false
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

    private String readThemeCss(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry(THEME_CSS_ENTRY);
            if (entry == null) {
                return null;
            }
            try (var is = jarFile.getInputStream(entry)) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to read theme.css from {}", jarPath, e);
            return null;
        }
    }

    private String readState() {
        Path path = Paths.get(statePath);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String content = Files.readString(path).strip();
            return content.isBlank() ? null : content;
        } catch (IOException e) {
            log.warn("Failed to read theme state from {}", path, e);
            return null;
        }
    }

    private void writeState(String id) {
        Path path = Paths.get(statePath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempFile = parent == null
                ? Files.createTempFile("theme-state-", ".tmp")
                : Files.createTempFile(parent, "theme-state-", ".tmp");
            try {
                Files.writeString(tempFile, id);
                try {
                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist active theme to " + path, e);
        }
    }
}
