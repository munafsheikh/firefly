package ai.firefly.plugin.markdown;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/markdown")
@RequiredArgsConstructor
public class MarkdownController {

    private final MarkdownFileService fileService;
    private final MarkdownRenderer renderer;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("plugin", "markdown", "status", "ok"));
    }

    @GetMapping("/files")
    public ResponseEntity<List<MarkdownFileInfo>> listFiles() {
        return ResponseEntity.ok(fileService.listFiles());
    }

    @GetMapping("/files/{*path}")
    public ResponseEntity<String> readFile(@PathVariable("path") String path) {
        // Both a missing file and a path that escapes the root are reported as 404,
        // so a traversal attempt cannot be distinguished from "no such file" and leak nothing.
        try {
            String content = fileService.readFile(relativize(path));
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(content);
        } catch (MarkdownPathTraversalException | MarkdownFileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/files/{*path}")
    public ResponseEntity<Void> writeFile(@PathVariable("path") String path, @RequestBody(required = false) String content) {
        fileService.writeFile(relativize(path), content);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/files/{*path}")
    public ResponseEntity<Void> deleteFile(@PathVariable("path") String path) {
        fileService.deleteFile(relativize(path));
        return ResponseEntity.noContent().build();
    }

    /**
     * Spring's "{@code {*path}}" catch-all capture always includes a leading "/" (it captures
     * everything after "/files" verbatim, boundary slash included). Strip exactly that one
     * artifact slash here so {@link MarkdownFileService#resolve} sees a plain root-relative
     * path and can tell a real absolute-path escape attempt apart from routing plumbing.
     */
    private static String relativize(String capturedPath) {
        return (capturedPath != null && capturedPath.startsWith("/")) ? capturedPath.substring(1) : capturedPath;
    }

    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@RequestBody(required = false) String markdown) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(renderer.renderToHtml(markdown));
    }

    @ExceptionHandler(MarkdownPathTraversalException.class)
    public ResponseEntity<Map<String, String>> handleTraversal(MarkdownPathTraversalException e) {
        log.warn("Rejected markdown path traversal attempt: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MarkdownFileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MarkdownFileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, String>> handleIoError(UncheckedIOException e) {
        log.error("Markdown file operation failed", e);
        return ResponseEntity.internalServerError().body(Map.of("error", "I/O error: " + e.getMessage()));
    }
}
