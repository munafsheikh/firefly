package ai.firefly.plugin.markdown;

import java.time.Instant;

/**
 * Metadata for a single Markdown file, relative to the configured root directory.
 */
public record MarkdownFileInfo(String path, long size, Instant lastModified) {
}
