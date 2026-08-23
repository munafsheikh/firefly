package ai.firefly.plugin.bashrunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenTargetResolverTest {

    private final OpenTargetResolver resolver = new OpenTargetResolver();

    @TempDir
    Path tempDir;

    @Test
    void resolvesExplicitMarkerRelativeToScriptDirectory() throws Exception {
        Path result = Files.writeString(tempDir.resolve("report.html"), "<h1>ok</h1>");

        assertThat(resolver.resolve("done\nFIREFLY_OPEN=report.html\n", tempDir))
                .contains(result.toAbsolutePath().normalize().toString());
    }

    @Test
    void fallsBackToLastNonBlankExistingPath() throws Exception {
        Path result = Files.writeString(tempDir.resolve("result.txt"), "ok");

        assertThat(resolver.resolve("generated\nresult.txt\n", tempDir))
                .contains(result.toAbsolutePath().normalize().toString());
    }

    @Test
    void acceptsHttpUrlsButRejectsUnsupportedSchemes() {
        assertThat(resolver.resolve("FIREFLY_OPEN=https://example.com/report\n", tempDir))
                .contains("https://example.com/report");
        assertThat(resolver.resolve("FIREFLY_OPEN=javascript:alert(1)\n", tempDir)).isEmpty();
    }
}
