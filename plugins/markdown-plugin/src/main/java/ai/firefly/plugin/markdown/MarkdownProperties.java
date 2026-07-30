package ai.firefly.plugin.markdown;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firefly.plugin.markdown")
public class MarkdownProperties {

    /** Whether the Markdown plugin is enabled. */
    private boolean enabled = true;

    /** Root directory that all Markdown file operations are scoped to. Created on first use if missing. */
    private String rootDir = System.getProperty("user.home") + "/.firefly/markdown";
}
