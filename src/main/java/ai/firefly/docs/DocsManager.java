package ai.firefly.docs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Aggregates documentation for the in-app Documentation Browser ({@code /docs}): a fixed set of
 * core topics bundled in the core app's own classpath under {@code docs/<sectionId>/}, plus one
 * section per installed plugin that bundles a {@code META-INF/firefly/docs/index.md} in its JAR
 * (discovered the same way {@code DashboardService} and {@code ThemeManager} scan the plugins
 * directory for their own conventions).
 *
 * <p>Each section's docs root may contain an {@code index.md} plus a {@code screenshots/}
 * subdirectory of images referenced by relative path from the markdown.
 */
@Slf4j
@Service
public class DocsManager {

    private static final String PLUGIN_PROPERTIES_ENTRY = "META-INF/plugin.properties";
    private static final String PLUGIN_DOCS_PREFIX = "META-INF/firefly/docs/";
    private static final String CORE_DOCS_PREFIX = "docs/";
    private static final String INDEX_PAGE = "index.md";

    private static final List<DocSection> CORE_SECTIONS = List.of(
        new DocSection("overview", "Overview", "Core", "What Firefly is and how the pieces fit together"),
        new DocSection("dashboard", "Web Dashboard", "Core", "The Thymeleaf dashboard: plugin browser, themes, system status"),
        new DocSection("terminal", "Web Terminal", "Core", "xterm.js + pty4j web terminal and the TamboUI TUI it hosts"),
        new DocSection("theme-manager", "Theme Manager", "Core", "How theme plugins are discovered and applied"),
        new DocSection("mcp-server", "Model Context Protocol", "Core", "The embedded MCP surface and the plugin skill/server tree")
    );

    @Value("${firefly.plugins.path:plugins}")
    private String pluginsPath;

    public List<DocSection> listSections() {
        List<DocSection> sections = new ArrayList<>(CORE_SECTIONS);
        Path dir = Paths.get(pluginsPath);
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
                for (Path jarPath : stream) {
                    DocSection section = readPluginSection(jarPath);
                    if (section != null) {
                        sections.add(section);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to scan plugins directory for docs: {}", dir, e);
            }
        }
        return sections;
    }

    public Optional<String> readMarkdown(String sectionId, String page) {
        if (isCoreSection(sectionId)) {
            return readClasspathText(CORE_DOCS_PREFIX + sectionId + "/" + page);
        }
        return findPluginJar(sectionId).flatMap(jarPath -> readJarEntryText(jarPath, PLUGIN_DOCS_PREFIX + page));
    }

    public Optional<DocAsset> readAsset(String sectionId, String assetPath) {
        String contentType = guessContentType(assetPath);
        if (isCoreSection(sectionId)) {
            return readClasspathBytes(CORE_DOCS_PREFIX + sectionId + "/" + assetPath)
                .map(bytes -> new DocAsset(bytes, contentType));
        }
        return findPluginJar(sectionId)
            .flatMap(jarPath -> readJarEntryBytes(jarPath, PLUGIN_DOCS_PREFIX + assetPath))
            .map(bytes -> new DocAsset(bytes, contentType));
    }

    private boolean isCoreSection(String sectionId) {
        return CORE_SECTIONS.stream().anyMatch(s -> s.id().equals(sectionId));
    }

    private DocSection readPluginSection(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            if (jarFile.getJarEntry(PLUGIN_DOCS_PREFIX + INDEX_PAGE) == null) {
                return null;
            }
            Properties props = new Properties();
            var propsEntry = jarFile.getJarEntry(PLUGIN_PROPERTIES_ENTRY);
            if (propsEntry != null) {
                try (var is = jarFile.getInputStream(propsEntry)) {
                    props.load(is);
                }
            }
            return new DocSection(
                props.getProperty("plugin.id", jarPath.getFileName().toString()),
                props.getProperty("plugin.name", "Unknown Plugin"),
                "Plugin",
                props.getProperty("plugin.description", "")
            );
        } catch (IOException e) {
            log.warn("Failed to read docs from {}", jarPath, e);
            return null;
        }
    }

    private Optional<Path> findPluginJar(String pluginId) {
        Path dir = Paths.get(pluginsPath);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path jarPath : stream) {
                try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                    var propsEntry = jarFile.getJarEntry(PLUGIN_PROPERTIES_ENTRY);
                    if (propsEntry == null) {
                        continue;
                    }
                    Properties props = new Properties();
                    try (var is = jarFile.getInputStream(propsEntry)) {
                        props.load(is);
                    }
                    if (pluginId.equals(props.getProperty("plugin.id"))) {
                        return Optional.of(jarPath);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to locate plugin JAR for docs section {}", pluginId, e);
        }
        return Optional.empty();
    }

    private Optional<String> readClasspathText(String classpathLocation) {
        return readClasspathBytes(classpathLocation).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    private Optional<byte[]> readClasspathBytes(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (var is = resource.getInputStream()) {
            return Optional.of(is.readAllBytes());
        } catch (IOException e) {
            log.warn("Failed to read core doc resource {}", classpathLocation, e);
            return Optional.empty();
        }
    }

    private Optional<String> readJarEntryText(Path jarPath, String entryName) {
        return readJarEntryBytes(jarPath, entryName).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    private Optional<byte[]> readJarEntryBytes(Path jarPath, String entryName) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                return Optional.empty();
            }
            try (var is = jarFile.getInputStream(entry)) {
                return Optional.of(is.readAllBytes());
            }
        } catch (IOException e) {
            log.warn("Failed to read {} from {}", entryName, jarPath, e);
            return Optional.empty();
        }
    }

    private String guessContentType(String path) {
        String type = URLConnection.guessContentTypeFromName(path);
        return type != null ? type : "application/octet-stream";
    }
}
