package ai.firefly.plugin.bashrunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BashRunnerServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void waitsForSuccessfulScriptAndResolvesOpenTarget() throws Exception {
        Path scripts = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(scripts.resolve("render.sh"), "#!/usr/bin/env bash\nset -e\nprintf '<h1>%s</h1>' \"$1\" > report.html\necho FIREFLY_OPEN=report.html\n");

        BashRunnerService service = service(scripts, Duration.ofSeconds(5));
        BashRunResult result = service.run(new BashRunRequest("render.sh", List.of("hello world"), false));

        assertThat(result.exitCode()).isZero();
        assertThat(result.timedOut()).isFalse();
        assertThat(result.stdout()).contains("FIREFLY_OPEN=report.html");
        assertThat(result.openTarget()).isEqualTo(scripts.resolve("report.html").toAbsolutePath().normalize().toString());
        assertThat(Files.readString(scripts.resolve("report.html"))).isEqualTo("<h1>hello world</h1>");
        assertThat(result.openAttempted()).isFalse();
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        Path scripts = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(tempDir.resolve("evil.sh"), "echo nope\n");
        BashRunnerService service = service(scripts, Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.run(new BashRunRequest("../evil.sh", List.of(), false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes");
    }

    @Test
    void doesNotResolveOrOpenResultWhenScriptFails() throws Exception {
        Path scripts = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(scripts.resolve("fail.sh"), "echo FIREFLY_OPEN=https://example.com\nexit 7\n");
        BashRunnerService service = service(scripts, Duration.ofSeconds(5));

        BashRunResult result = service.run(new BashRunRequest("fail.sh", List.of(), true));

        assertThat(result.exitCode()).isEqualTo(7);
        assertThat(result.openTarget()).isNull();
        assertThat(result.openAttempted()).isFalse();
    }

    @Test
    void timesOutLongRunningScript() throws Exception {
        Path scripts = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(scripts.resolve("slow.sh"), "sleep 5\necho FIREFLY_OPEN=https://example.com\n");
        BashRunnerService service = service(scripts, Duration.ofMillis(100));

        BashRunResult result = service.run(new BashRunRequest("slow.sh", List.of(), true));

        assertThat(result.timedOut()).isTrue();
        assertThat(result.openAttempted()).isFalse();
        assertThat(result.openTarget()).isNull();
    }

    @Test
    void listsOnlyRegularShellScripts() throws Exception {
        Path scripts = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(scripts.resolve("a.sh"), "echo a\n");
        Files.writeString(scripts.resolve("ignore.txt"), "ignore\n");
        Files.createDirectories(scripts.resolve("nested"));
        Files.writeString(scripts.resolve("nested/b.sh"), "echo b\n");

        assertThat(service(scripts, Duration.ofSeconds(5)).listScripts())
                .extracting(ScriptInfo::path)
                .containsExactly("a.sh", "nested/b.sh");
    }

    private BashRunnerService service(Path scriptsRoot, Duration timeout) {
        BashRunnerProperties properties = new BashRunnerProperties();
        properties.setScriptsRoot(scriptsRoot.toString());
        properties.setTimeout(timeout);
        properties.setXdgOpenEnabled(false);
        return new BashRunnerService(properties, new OpenTargetResolver(), new XdgOpenService(properties));
    }
}
