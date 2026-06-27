package ai.firefly.testdoc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional override for {@link ScreenshotDocumentationExtension}.
 *
 * <p>The extension already screenshots every test that boots a real embedded web server
 * (e.g. {@code @SpringBootTest(webEnvironment = RANDOM_PORT)}), defaulting to the {@code /}
 * path. Apply this annotation only when a test wants extra/different pages captured.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DocScreenshot {

    /**
     * Paths (relative to the embedded server's base URL) to capture. Defaults to the root page.
     */
    String[] paths() default {"/"};
}
