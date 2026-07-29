package ai.firefly.plugin.webtui;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the WebTUI Browser plugin.
 *
 * <p>Guarded the same way {@code ai.firefly.terminal.TerminalAutoConfiguration} guards pty4j: gated on
 * both the Playwright classpath ({@code @ConditionalOnClass}) and the {@code firefly.plugin.webtui.enabled}
 * property, so the whole plugin simply doesn't activate if Playwright isn't on the classpath — and even
 * when it is, {@link BrowserSessionService} degrades gracefully if headless Chromium itself can't launch.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "com.microsoft.playwright.Playwright")
@EnableConfigurationProperties(WebtuiProperties.class)
@ConditionalOnProperty(prefix = "firefly.plugin.webtui", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebtuiPluginAutoConfiguration {

    private BrowserSessionService browserSessionService;

    public WebtuiPluginAutoConfiguration() {
        log.info("Firefly WebTUI Browser Plugin auto-configuration loaded.");
    }

    @Bean
    public BrowserSessionService webtuiBrowserSessionService(WebtuiProperties properties) {
        this.browserSessionService = new BrowserSessionService(properties);
        return this.browserSessionService;
    }

    @Bean
    public WebtuiPluginController webtuiPluginController(BrowserSessionService browserSessionService) {
        return new WebtuiPluginController(browserSessionService);
    }

    @Bean
    public WebtuiPageController webtuiPageController(WebtuiProperties properties) {
        return new WebtuiPageController(properties);
    }

    @PreDestroy
    public void shutdown() {
        if (browserSessionService != null) {
            browserSessionService.shutdown();
        }
    }
}
