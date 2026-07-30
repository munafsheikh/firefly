package ai.firefly.plugin.markdown;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MarkdownProperties.class)
@ConditionalOnProperty(prefix = "firefly.plugin.markdown", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MarkdownPluginAutoConfiguration {

    public MarkdownPluginAutoConfiguration() {
        log.info("Firefly Markdown Plugin auto-configuration loaded.");
    }

    @Bean
    public MarkdownFileService markdownFileService(MarkdownProperties properties) {
        return new MarkdownFileService(properties);
    }

    @Bean
    public MarkdownRenderer markdownRenderer() {
        return new MarkdownRenderer();
    }

    @Bean
    public MarkdownController markdownController(MarkdownFileService fileService, MarkdownRenderer renderer) {
        return new MarkdownController(fileService, renderer);
    }

    @Bean
    public MarkdownPageController markdownPageController() {
        return new MarkdownPageController();
    }
}
