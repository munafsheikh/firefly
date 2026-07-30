package ai.firefly.plugin.markdown;

/**
 * Thrown when a requested relative path would resolve to a location outside the
 * configured Markdown root directory (e.g. "../" traversal, absolute paths, or a
 * symlink that escapes the root).
 */
public class MarkdownPathTraversalException extends RuntimeException {

    public MarkdownPathTraversalException(String message) {
        super(message);
    }
}
