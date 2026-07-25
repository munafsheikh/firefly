package ai.firefly.testdoc;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.AnnotatedElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auto-detected JUnit 5 extension (see {@code META-INF/services} +
 * {@code junit-platform.properties}) that screenshots the running app for every test that has
 * a visually detectable outcome — i.e. any Spring test that boots a real embedded web server
 * ({@code @SpringBootTest(webEnvironment = RANDOM_PORT/DEFINED_PORT)}), exposing
 * {@code local.server.port}.
 *
 * <p>Tests with no such server (plain unit tests, {@code MOCK}/{@code NONE} web environments)
 * are silently skipped — there is nothing to render. Any Playwright failure (browser not
 * installed, no network for the first download, etc.) is swallowed so this never breaks a
 * test run; it only adds documentation on top of tests that already pass or fail on their own
 * merits.
 */
public class ScreenshotDocumentationExtension implements AfterTestExecutionCallback {

    private static final Logger LOG = Logger.getLogger(ScreenshotDocumentationExtension.class.getName());
    private static final Path OUTPUT_ROOT = Path.of("docs", "screenshots");
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ScreenshotDocumentationExtension.class);

    private static volatile boolean disabledAfterFailure = false;

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (disabledAfterFailure) {
            return;
        }

        Optional<String> baseUrl = resolveBaseUrl(context);
        if (baseUrl.isEmpty()) {
            return;
        }

        Browser browser = sharedBrowser(context);
        if (browser == null) {
            return;
        }

        String outcome = context.getExecutionException().isPresent() ? "FAIL" : "PASS";
        String testClass = context.getRequiredTestClass().getSimpleName();
        String testMethod = context.getRequiredTestMethod().getName();

        for (String relativePath : pathsFor(context)) {
            captureScreenshot(browser, baseUrl.get(), relativePath, testClass, testMethod, outcome);
        }
    }

    private void captureScreenshot(Browser browser, String baseUrl, String relativePath,
                                    String testClass, String testMethod, String outcome) {
        try (Page page = browser.newPage()) {
            page.navigate(baseUrl + relativePath);
            try {
                // 3 s cap: WebSocket-backed pages (terminal) never reach NETWORKIDLE, so we don't wait forever.
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(3000));
            } catch (Exception networkNeverIdle) {
                // Expected for pages with persistent connections (terminal, live-reload, etc.).
            }
            Path dir = OUTPUT_ROOT.resolve(testClass);
            java.nio.file.Files.createDirectories(dir);
            String fileName = "%s__%s__%s.png".formatted(testMethod, outcome, sanitize(relativePath));
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(dir.resolve(fileName))
                    .setFullPage(true));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Skipping doc screenshot for " + testClass + "#" + testMethod
                    + " (" + relativePath + "): " + e.getMessage());
        }
    }

    private static String sanitize(String relativePath) {
        String cleaned = relativePath.replaceAll("[^a-zA-Z0-9]+", "-");
        return cleaned.isBlank() ? "root" : cleaned;
    }

    private static List<String> pathsFor(ExtensionContext context) {
        Optional<DocScreenshot> methodAnnotation =
                findAnnotation(context.getRequiredTestMethod(), DocScreenshot.class);
        if (methodAnnotation.isPresent()) {
            return List.of(methodAnnotation.get().paths());
        }
        Optional<DocScreenshot> classAnnotation =
                findAnnotation(context.getRequiredTestClass(), DocScreenshot.class);
        return List.of(classAnnotation.map(DocScreenshot::paths).orElse(new String[] {"/"}));
    }

    private static <T extends java.lang.annotation.Annotation> Optional<T> findAnnotation(
            AnnotatedElement element, Class<T> type) {
        return Optional.ofNullable(element.getAnnotation(type));
    }

    /**
     * Returns the base URL of the embedded web server for this test, or empty if the test
     * didn't boot one (i.e. has no visually detectable outcome to screenshot).
     */
    private static Optional<String> resolveBaseUrl(ExtensionContext context) {
        ApplicationContext applicationContext;
        try {
            applicationContext = SpringExtension.getApplicationContext(context);
        } catch (IllegalStateException notASpringTest) {
            return Optional.empty();
        }

        Environment env = applicationContext.getEnvironment();
        String port = env.getProperty("local.server.port");
        if (port == null || port.isBlank() || "-1".equals(port)) {
            return Optional.empty();
        }
        return Optional.of("http://localhost:" + port);
    }

    private static Browser sharedBrowser(ExtensionContext context) {
        ExtensionContext.Store rootStore = context.getRoot().getStore(NAMESPACE);
        try {
            PlaywrightResource resource = rootStore.getOrComputeIfAbsent(
                    PlaywrightResource.class, key -> new PlaywrightResource(), PlaywrightResource.class);
            return resource.browser;
        } catch (Exception e) {
            disabledAfterFailure = true;
            LOG.log(Level.WARNING,
                    "Disabling doc screenshots for this run: Playwright/Chromium unavailable ("
                            + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * One Playwright + Browser instance per test run, closed automatically by JUnit when the
     * root context tears down.
     */
    private static final class PlaywrightResource
            implements ExtensionContext.Store.CloseableResource, AutoCloseable {
        private final Playwright playwright;
        private final Browser browser;

        PlaywrightResource() {
            this.playwright = Playwright.create();
            this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        }

        @Override
        public void close() {
            browser.close();
            playwright.close();
        }
    }
}
