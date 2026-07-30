package ai.firefly.docs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocsManagerTest {

    @TempDir
    Path pluginsDir;

    private DocsManager docsManager;

    @BeforeEach
    void setUp() {
        docsManager = new DocsManager();
        ReflectionTestUtils.setField(docsManager, "pluginsPath", pluginsDir.toString());
    }

    @Test
    void listSectionsAlwaysIncludesCoreTopics() {
        List<DocSection> sections = docsManager.listSections();

        assertThat(sections).extracting(DocSection::id)
            .contains("overview", "dashboard", "terminal", "theme-manager", "mcp-server");
        assertThat(sections).allMatch(s -> "Core".equals(s.category()));
    }

    @Test
    void listSectionsIncludesPluginsBundlingDocs() throws IOException {
        writeDocsPluginJar(pluginsDir.resolve("sample-plugin.jar"), "sample", "Sample Plugin", "# Sample\n\nHello.");

        List<DocSection> sections = docsManager.listSections();

        assertThat(sections).extracting(DocSection::id).contains("sample");
        assertThat(sections.stream().filter(s -> s.id().equals("sample")).findFirst().orElseThrow().category())
            .isEqualTo("Plugin");
    }

    @Test
    void listSectionsExcludesPluginsWithoutDocs() throws IOException {
        writePluginJarWithoutDocs(pluginsDir.resolve("no-docs-plugin.jar"), "no-docs", "No Docs Plugin");

        List<DocSection> sections = docsManager.listSections();

        assertThat(sections).extracting(DocSection::id).doesNotContain("no-docs");
    }

    @Test
    void readMarkdownReturnsCoreDoc() {
        var markdown = docsManager.readMarkdown("overview", "index.md");

        assertThat(markdown).isPresent();
        assertThat(markdown.get()).contains("Firefly");
    }

    @Test
    void readMarkdownReturnsPluginDoc() throws IOException {
        writeDocsPluginJar(pluginsDir.resolve("sample-plugin.jar"), "sample", "Sample Plugin", "# Sample\n\nHello.");

        var markdown = docsManager.readMarkdown("sample", "index.md");

        assertThat(markdown).contains("# Sample\n\nHello.");
    }

    @Test
    void readMarkdownIsEmptyForUnknownSection() {
        assertThat(docsManager.readMarkdown("does-not-exist", "index.md")).isEmpty();
    }

    @Test
    void readAssetReturnsPluginScreenshot() throws IOException {
        Path jarPath = pluginsDir.resolve("sample-plugin.jar");
        byte[] fakePng = {(byte) 0x89, 'P', 'N', 'G'};
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
            writeEntry(jos, "plugin.id=sample\nplugin.name=Sample Plugin\n");
            jos.putNextEntry(new JarEntry("META-INF/firefly/docs/index.md"));
            writeEntry(jos, "# Sample");
            jos.putNextEntry(new JarEntry("META-INF/firefly/docs/screenshots/example.png"));
            jos.write(fakePng);
            jos.closeEntry();
        }

        var asset = docsManager.readAsset("sample", "screenshots/example.png");

        assertThat(asset).isPresent();
        assertThat(asset.get().bytes()).isEqualTo(fakePng);
        assertThat(asset.get().contentType()).isEqualTo("image/png");
    }

    private void writeDocsPluginJar(Path jarPath, String id, String name, String indexMarkdown) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
            writeEntry(jos, "plugin.id=" + id + "\nplugin.name=" + name + "\n");
            jos.putNextEntry(new JarEntry("META-INF/firefly/docs/index.md"));
            writeEntry(jos, indexMarkdown);
        }
    }

    private void writePluginJarWithoutDocs(Path jarPath, String id, String name) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
            writeEntry(jos, "plugin.id=" + id + "\nplugin.name=" + name + "\n");
        }
    }

    private void writeEntry(JarOutputStream jos, String content) throws IOException {
        jos.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }
}
