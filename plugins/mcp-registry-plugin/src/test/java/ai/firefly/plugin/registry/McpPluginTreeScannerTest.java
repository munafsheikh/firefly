package ai.firefly.plugin.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class McpPluginTreeScannerTest {

    @TempDir
    Path pluginsDir;

    private McpPluginTreeScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new McpPluginTreeScanner();
        ReflectionTestUtils.setField(scanner, "pluginsPath", pluginsDir.toString());
    }

    @Test
    void scanTreeGroupsSkillsAndServersByPlugin() throws IOException {
        writePluginJar(pluginsDir.resolve("sample-plugin.jar"), "sample", "Sample Plugin", "1.2.3", """
            {
              "skills": [{"name": "sample-skill", "description": "does a thing"}],
              "servers": [{"name": "sample-http", "type": "http", "target": "/api/sample", "description": "sample server"}]
            }
            """);

        List<McpPluginNode> tree = scanner.scanTree();

        assertThat(tree).hasSize(1);
        McpPluginNode node = tree.get(0);
        assertThat(node.pluginId()).isEqualTo("sample");
        assertThat(node.pluginName()).isEqualTo("Sample Plugin");
        assertThat(node.pluginVersion()).isEqualTo("1.2.3");
        assertThat(node.skills()).extracting(SkillDefinition::name).containsExactly("sample-skill");
        assertThat(node.servers()).extracting(McpServerNodeDefinition::name).containsExactly("sample-http");
    }

    @Test
    void scanTreeExcludesPluginsWithoutManifest() throws IOException {
        writePluginJar(pluginsDir.resolve("no-manifest-plugin.jar"), "no-manifest", "No Manifest Plugin", "1.0.0", null);

        List<McpPluginNode> tree = scanner.scanTree();

        assertThat(tree).isEmpty();
    }

    @Test
    void readPluginPropertiesFindsMatchingPlugin() throws IOException {
        writePluginJar(pluginsDir.resolve("sample-plugin.jar"), "sample", "Sample Plugin", "1.2.3",
            "{\"skills\":[{\"name\":\"x\",\"description\":\"y\"}]}");

        Properties props = scanner.readPluginProperties("sample");

        assertThat(props.getProperty("plugin.name")).isEqualTo("Sample Plugin");
    }

    private void writePluginJar(Path jarPath, String id, String name, String version, String manifestJson) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(java.nio.file.Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
            String props = "plugin.id=" + id + "\nplugin.name=" + name + "\nplugin.version=" + version + "\n";
            writeEntry(jos, props);

            if (manifestJson != null) {
                jos.putNextEntry(new JarEntry("META-INF/firefly/mcp-plugin.json"));
                writeEntry(jos, manifestJson);
            }
        }
    }

    private void writeEntry(JarOutputStream jos, String content) throws IOException {
        OutputStream noCloseWrapper = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                jos.write(b);
            }
        };
        noCloseWrapper.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }
}
