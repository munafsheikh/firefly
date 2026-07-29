package ai.firefly.plugin.markdown;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownFileServiceTest {

    @TempDir
    Path tempDir;

    private MarkdownFileService service;

    @BeforeEach
    void setUp() {
        MarkdownProperties properties = new MarkdownProperties();
        properties.setRootDir(tempDir.resolve("markdown-root").toString());
        service = new MarkdownFileService(properties);
    }

    @Test
    void createsRootDirectoryIfMissing() {
        assertThat(Files.isDirectory(service.getRoot())).isTrue();
    }

    @Test
    void writesReadsAndDeletesAFile() {
        service.writeFile("notes/todo.md", "# Todo\n\n- one\n- two\n");

        assertThat(service.readFile("notes/todo.md")).isEqualTo("# Todo\n\n- one\n- two\n");

        service.deleteFile("notes/todo.md");

        assertThatThrownBy(() -> service.readFile("notes/todo.md"))
            .isInstanceOf(MarkdownFileNotFoundException.class);
    }

    @Test
    void overwritesExistingFile() {
        service.writeFile("a.md", "first");
        service.writeFile("a.md", "second");

        assertThat(service.readFile("a.md")).isEqualTo("second");
    }

    @Test
    void listsOnlyMarkdownFilesRecursively() {
        service.writeFile("a.md", "a");
        service.writeFile("nested/b.md", "b");
        service.writeFile("nested/deeper/c.md", "c");
        service.writeFile("not-markdown.txt", "ignored");

        List<MarkdownFileInfo> files = service.listFiles();

        assertThat(files).extracting(MarkdownFileInfo::path)
            .containsExactlyInAnyOrder("a.md", "nested/b.md", "nested/deeper/c.md");
        assertThat(files).allSatisfy(f -> assertThat(f.size()).isGreaterThan(0));
    }

    @Test
    void readingMissingFileThrowsNotFound() {
        assertThatThrownBy(() -> service.readFile("missing.md"))
            .isInstanceOf(MarkdownFileNotFoundException.class);
    }

    @Test
    void deletingMissingFileThrowsNotFound() {
        assertThatThrownBy(() -> service.deleteFile("missing.md"))
            .isInstanceOf(MarkdownFileNotFoundException.class);
    }

    @Test
    void rejectsParentDirectoryTraversal() {
        assertThatThrownBy(() -> service.resolve("../../etc/passwd"))
            .isInstanceOf(MarkdownPathTraversalException.class);

        assertThatThrownBy(() -> service.readFile("../../etc/passwd"))
            .isInstanceOf(MarkdownPathTraversalException.class);
    }

    @Test
    void rejectsTraversalHiddenInsideANormallyLookingPath() {
        assertThatThrownBy(() -> service.resolve("notes/../../secrets.md"))
            .isInstanceOf(MarkdownPathTraversalException.class);
    }

    @Test
    void rejectsAbsolutePaths() {
        assertThatThrownBy(() -> service.resolve("/etc/passwd"))
            .isInstanceOf(MarkdownPathTraversalException.class);
    }

    @Test
    void rejectsBlankPath() {
        assertThatThrownBy(() -> service.resolve("   "))
            .isInstanceOf(MarkdownPathTraversalException.class);
        assertThatThrownBy(() -> service.resolve(""))
            .isInstanceOf(MarkdownPathTraversalException.class);
    }

    @Test
    void rejectsSymlinkThatEscapesRoot() throws IOException {
        Path outsideDir = Files.createDirectory(tempDir.resolve("outside"));
        Path secretFile = outsideDir.resolve("secret.md");
        Files.writeString(secretFile, "top secret");

        Path linkInsideRoot = service.getRoot().resolve("escape-link");
        try {
            Files.createSymbolicLink(linkInsideRoot, outsideDir);
        } catch (UnsupportedOperationException | IOException e) {
            // Some CI sandboxes disallow creating symlinks; skip rather than fail spuriously.
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symlinks not supported in this environment");
            return;
        }

        assertThatThrownBy(() -> service.readFile("escape-link/secret.md"))
            .isInstanceOf(MarkdownPathTraversalException.class);
    }

    @Test
    void writingCreatesParentDirectoriesUnderRoot() {
        service.writeFile("deep/nested/dir/file.md", "content");

        assertThat(Files.isRegularFile(service.getRoot().resolve("deep/nested/dir/file.md"))).isTrue();
        assertThat(service.readFile("deep/nested/dir/file.md")).isEqualTo("content");
    }
}
