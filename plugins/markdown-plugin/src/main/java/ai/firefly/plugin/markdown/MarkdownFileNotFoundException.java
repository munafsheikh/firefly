package ai.firefly.plugin.markdown;

/**
 * Thrown when a requested Markdown file does not exist under the configured root.
 */
public class MarkdownFileNotFoundException extends RuntimeException {

    public MarkdownFileNotFoundException(String message) {
        super(message);
    }
}
