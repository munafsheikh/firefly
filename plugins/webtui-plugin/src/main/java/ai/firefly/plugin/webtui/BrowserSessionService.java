package ai.firefly.plugin.webtui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ScreenshotType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Lazily launches and owns a single shared headless Chromium {@link Browser} / {@link BrowserContext} /
 * {@link Page} via Playwright, and exposes the small set of operations the WebTUI Browser plugin needs:
 * navigate, screenshot, click, scroll, type, back, forward.
 *
 * <p>This is a scoped-down "browse with real JS/CSS/images rendered as a screenshot" MVP, not a full
 * remote-desktop protocol: there is exactly one shared page for the whole plugin, and only one browser
 * operation runs at a time.
 *
 * <p>Playwright's Java bindings are not thread-safe — every call touching a {@link Playwright},
 * {@link Browser}, {@link BrowserContext} or {@link Page} instance must happen on the same thread that
 * created it. This service pins all Playwright work to a single dedicated background thread and funnels
 * every public method through it.
 *
 * <p>If headless Chromium can't actually be launched in the current environment (e.g. the slim JRE
 * runtime image lacks the native shared libraries Chromium needs), initialization catches the failure,
 * flips {@link #isAvailable()} to {@code false}, and every subsequent operation fails fast with a
 * {@link BrowserUnavailableException} instead of throwing on every call or crashing the plugin.
 */
@Slf4j
public class BrowserSessionService {

    private final WebtuiProperties properties;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "webtui-playwright");
        thread.setDaemon(true);
        return thread;
    });

    private final Object initGate = new Object();
    private volatile Future<?> initFuture;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private volatile boolean available = false;
    private volatile String unavailableReason = "Browser has not been initialized yet";

    public BrowserSessionService(WebtuiProperties properties) {
        this.properties = properties;
    }

    /** Whether a real headless Chromium instance is up and usable. Triggers lazy init on first call. */
    public boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    /** Human-readable reason the browser is unavailable, if it is. */
    public String getUnavailableReason() {
        ensureInitialized();
        return unavailableReason;
    }

    public record PageInfo(String url, String title) {
    }

    public PageInfo navigate(String url) {
        requireAvailable();
        return submit(() -> {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(properties.getNavigationTimeoutSeconds() * 1000.0));
            return new PageInfo(page.url(), page.title());
        });
    }

    public byte[] screenshot() {
        requireAvailable();
        return submit(() -> page.screenshot(new Page.ScreenshotOptions().setType(ScreenshotType.PNG)));
    }

    public void click(double x, double y) {
        requireAvailable();
        submit(() -> {
            page.mouse().click(x, y);
            return null;
        });
    }

    public void scroll(double deltaY) {
        requireAvailable();
        submit(() -> {
            page.mouse().wheel(0, deltaY);
            return null;
        });
    }

    public void type(String text) {
        requireAvailable();
        submit(() -> {
            page.keyboard().type(text);
            return null;
        });
    }

    public PageInfo goBack() {
        requireAvailable();
        return submit(() -> {
            page.goBack(new Page.GoBackOptions()
                    .setTimeout(properties.getNavigationTimeoutSeconds() * 1000.0));
            return new PageInfo(page.url(), page.title());
        });
    }

    public PageInfo goForward() {
        requireAvailable();
        return submit(() -> {
            page.goForward(new Page.GoForwardOptions()
                    .setTimeout(properties.getNavigationTimeoutSeconds() * 1000.0));
            return new PageInfo(page.url(), page.title());
        });
    }

    /** Cleanly tears down the Playwright browser/context/page on application shutdown. */
    public void shutdown() {
        Future<?> f = initFuture;
        if (f != null) {
            try {
                f.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // best-effort; we're shutting down regardless
            }
        }
        try {
            executor.submit(this::closeQuietly).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("WebTUI plugin: error while closing browser session on shutdown: {}", e.getMessage());
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private void requireAvailable() {
        ensureInitialized();
        if (!available) {
            throw new BrowserUnavailableException(unavailableReason);
        }
    }

    private void ensureInitialized() {
        Future<?> f = initFuture;
        if (f == null) {
            synchronized (initGate) {
                f = initFuture;
                if (f == null) {
                    f = executor.submit(this::doInitialize);
                    initFuture = f;
                }
            }
        }
        try {
            f.get();
        } catch (ExecutionException e) {
            // doInitialize() catches everything internally and never throws; defensive fallback only.
            available = false;
            unavailableReason = "Browser initialization failed: " + e.getCause();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            available = false;
            unavailableReason = "Browser initialization was interrupted";
        } catch (RejectedExecutionException e) {
            available = false;
            unavailableReason = "Browser session has been shut down";
        }
    }

    /** Runs on the dedicated Playwright thread. Never throws — flips {@link #available} instead. */
    private Void doInitialize() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(properties.getViewportWidth(), properties.getViewportHeight()));
            page = context.newPage();
            available = true;
            unavailableReason = null;
            log.info("WebTUI plugin: headless Chromium session started ({}x{})",
                    properties.getViewportWidth(), properties.getViewportHeight());
        } catch (Throwable t) {
            available = false;
            unavailableReason = "Headless Chromium is not available in this environment: " + t.getMessage();
            log.warn("WebTUI plugin: browser unavailable - {}", unavailableReason);
            closeQuietly();
        }
        return null;
    }

    private void closeQuietly() {
        try {
            if (page != null) {
                page.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        }
        page = null;
        context = null;
        browser = null;
        playwright = null;
    }

    private <T> T submit(Callable<T> task) {
        try {
            return executor.submit(task).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new WebtuiOperationException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebtuiOperationException("Browser operation was interrupted", e);
        } catch (RejectedExecutionException e) {
            throw new BrowserUnavailableException("Browser session has been shut down");
        }
    }

    /** Thrown when an operation is attempted but no working browser is available. */
    public static class BrowserUnavailableException extends RuntimeException {
        public BrowserUnavailableException(String message) {
            super(message);
        }
    }

    /** Thrown when the browser is available but a specific operation (navigate, click, ...) failed. */
    public static class WebtuiOperationException extends RuntimeException {
        public WebtuiOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
