package ai.firefly.plugin.markdown;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reads, writes, lists, and deletes Markdown files scoped to a configured root directory.
 *
 * <p>Every operation resolves the caller-supplied relative path against the root, normalizes
 * it, and verifies the resolved location is still inside the root (including a real-path check
 * against the nearest existing ancestor, to guard against symlink escapes) before touching the
 * filesystem. Any attempt to escape the root results in {@link MarkdownPathTraversalException}.</p>
 */
@Slf4j
public class MarkdownFileService {

    private final Path root;

    public MarkdownFileService(MarkdownProperties properties) {
        this.root = initRoot(properties.getRootDir());
    }

    private static Path initRoot(String rootDir) {
        try {
            Path path = Paths.get(rootDir).toAbsolutePath().normalize();
            Files.createDirectories(path);
            return path.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize markdown root directory: " + rootDir, e);
        }
    }

    /** The real, absolute root directory all operations are scoped to. */
    public Path getRoot() {
        return root;
    }

    /**
     * Resolves a caller-supplied relative path against the root, guaranteeing the result stays
     * inside it. Package-private so tests can exercise it directly.
     */
    Path resolve(String relativePath) {
        if (relativePath == null) {
            throw new MarkdownPathTraversalException("Path must not be null");
        }

        String cleaned = relativePath.replace('\\', '/');
        if (cleaned.isBlank()) {
            throw new MarkdownPathTraversalException("Path must not be blank");
        }
        // A leading slash here means a genuinely absolute path was supplied - callers coming
        // through the REST layer (MarkdownController) already strip the single leading slash
        // that Spring's "{*path}" catch-all capture always includes, so anything still
        // absolute at this point is rejected outright rather than silently reinterpreted
        // as root-relative.
        if (cleaned.startsWith("/")) {
            throw new MarkdownPathTraversalException("Absolute paths are not allowed: " + relativePath);
        }

        Path candidateRelative = Path.of(cleaned);
        if (candidateRelative.isAbsolute()) {
            throw new MarkdownPathTraversalException("Absolute paths are not allowed: " + relativePath);
        }

        Path resolved = root.resolve(candidateRelative).normalize();
        if (!isInsideRoot(resolved)) {
            throw new MarkdownPathTraversalException("Path escapes the configured root directory: " + relativePath);
        }

        // Guard against symlink escapes: find the nearest existing ancestor (the file itself,
        // or the deepest directory that already exists) and verify its real filesystem path is
        // still inside the root's real path.
        try {
            Path existingAncestor = resolved;
            while (existingAncestor != null && !Files.exists(existingAncestor)) {
                existingAncestor = existingAncestor.getParent();
            }
            if (existingAncestor != null) {
                Path realAncestor = existingAncestor.toRealPath();
                if (!isInsideRoot(realAncestor)) {
                    throw new MarkdownPathTraversalException(
                        "Path escapes the configured root directory via symlink: " + relativePath);
                }
            }
        } catch (IOException e) {
            throw new MarkdownPathTraversalException("Unable to verify path safety: " + relativePath);
        }

        return resolved;
    }

    private boolean isInsideRoot(Path candidate) {
        return candidate.equals(root) || candidate.startsWith(root);
    }

    public List<MarkdownFileInfo> listFiles() {
        List<MarkdownFileInfo> results = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return results;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                .sorted()
                .forEach(p -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        String relative = root.relativize(p).toString().replace('\\', '/');
                        results.add(new MarkdownFileInfo(relative, attrs.size(), attrs.lastModifiedTime().toInstant()));
                    } catch (IOException e) {
                        log.warn("Failed to read attributes for {}", p, e);
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list markdown files under " + root, e);
        }
        return results;
    }

    public String readFile(String relativePath) {
        Path resolved = resolve(relativePath);
        if (!Files.isRegularFile(resolved)) {
            throw new MarkdownFileNotFoundException("File not found: " + relativePath);
        }
        try {
            return Files.readString(resolved, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + relativePath, e);
        }
    }

    public void writeFile(String relativePath, String content) {
        Path resolved = resolve(relativePath);
        try {
            if (resolved.getParent() != null) {
                Files.createDirectories(resolved.getParent());
            }
            Files.writeString(resolved, content == null ? "" : content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write file: " + relativePath, e);
        }
    }

    public void deleteFile(String relativePath) {
        Path resolved = resolve(relativePath);
        if (!Files.exists(resolved)) {
            throw new MarkdownFileNotFoundException("File not found: " + relativePath);
        }
        try {
            Files.delete(resolved);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file: " + relativePath, e);
        }
    }
}
