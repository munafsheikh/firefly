package ai.firefly.docs;

import ai.firefly.testdoc.DocScreenshot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Boots the real app with a fake plugin JAR (bundling {@code META-INF/firefly/docs/index.md} +
 * a screenshot) dropped into a temp plugins directory, proving the Documentation Browser combines
 * core docs and plugin-contributed docs end to end over real HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DocScreenshot(paths = {"/docs"})
class DocsControllerIntegrationTest {

    private static final Path PLUGINS_DIR = createPluginsDir();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("firefly.plugins.path", PLUGINS_DIR::toString);
    }

    @Test
    void landingPageRendersACoreSection() {
        String body = get("/docs");

        assertThat(body).contains("Firefly Documentation");
        assertThat(body).contains("Web Dashboard");
    }

    @Test
    @DocScreenshot(paths = {"/docs/dashboard"})
    void coreSectionRendersItsMarkdown() {
        String body = get("/docs/dashboard");

        assertThat(body).contains("Dashboard");
    }

    @Test
    @DocScreenshot(paths = {"/docs/docs-test-plugin"})
    void pluginSectionIsListedAndRendersItsOwnMarkdown() {
        String nav = get("/docs");
        assertThat(nav).contains("Docs Test Plugin");

        String body = get("/docs/docs-test-plugin");
        assertThat(body).contains("Hello from the test plugin");
        assertThat(body).contains("/docs/docs-test-plugin/assets/screenshots/example.png");
    }

    @Test
    void pluginAssetIsServedWithCorrectContentType() {
        RestClient.ResponseSpec response = RestClient.create()
            .get()
            .uri("http://localhost:{port}/docs/docs-test-plugin/assets/screenshots/example.png", port)
            .retrieve();

        byte[] body = response.body(byte[].class);
        assertThat(body).isNotEmpty();
        assertThat(body[0]).isEqualTo((byte) 0x89); // PNG magic number first byte
    }

    @Test
    void unknownSectionAssetReturnsNotFound() {
        assertThatThrownBy(() -> RestClient.create()
            .get()
            .uri("http://localhost:{port}/docs/does-not-exist/assets/x.png", port)
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.NotFound.class)
            .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private String get(String path) {
        return RestClient.create()
            .get()
            .uri("http://localhost:{port}" + path, port)
            .retrieve()
            .body(String.class);
    }

    private static Path createPluginsDir() {
        try {
            Path dir = Files.createTempDirectory("docs-integration-plugins-");
            Path jarPath = dir.resolve("docs-test-plugin.jar");
            byte[] fakePng = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
            try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
                jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
                writeEntry(jos, "plugin.id=docs-test-plugin\nplugin.name=Docs Test Plugin\nplugin.version=1.0.0\n");

                jos.putNextEntry(new JarEntry("META-INF/firefly/docs/index.md"));
                writeEntry(jos, "# Docs Test Plugin\n\nHello from the test plugin.\n\n![Example](screenshots/example.png)\n");

                jos.putNextEntry(new JarEntry("META-INF/firefly/docs/screenshots/example.png"));
                jos.write(fakePng);
                jos.closeEntry();
            }
            return dir;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp plugins dir", e);
        }
    }

    private static void writeEntry(JarOutputStream jos, String content) throws IOException {
        jos.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }
}
