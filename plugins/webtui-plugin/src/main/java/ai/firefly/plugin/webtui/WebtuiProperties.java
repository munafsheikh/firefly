package ai.firefly.plugin.webtui;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the WebTUI Browser plugin.
 *
 * <p>Bound from {@code firefly.plugin.webtui.*} in application configuration.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "firefly.plugin.webtui")
public class WebtuiProperties {

    /** Master switch for the plugin's auto-configuration. */
    private boolean enabled = true;

    /** Default page the shared browser session starts on. */
    private String homepage = "https://example.com";

    /** Viewport width (pixels) for the headless browser context. */
    private int viewportWidth = 1280;

    /** Viewport height (pixels) for the headless browser context. */
    private int viewportHeight = 800;

    /** Timeout, in seconds, applied to navigation operations. */
    private int navigationTimeoutSeconds = 15;
}
