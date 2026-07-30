package ai.firefly.plugin.webtui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Plain unit tests (no Spring context) proving every {@link WebtuiPluginController} endpoint degrades to a
 * clean 503-with-JSON-body — never a 500 stack trace — when {@link BrowserSessionService#isAvailable()}
 * reports {@code false}.
 */
class WebtuiPluginControllerUnavailableTest {

    private BrowserSessionService browserSessionService;
    private WebtuiPluginController controller;

    @BeforeEach
    void setUp() {
        browserSessionService = mock(BrowserSessionService.class);
        when(browserSessionService.isAvailable()).thenReturn(false);
        when(browserSessionService.getUnavailableReason())
                .thenReturn("Headless Chromium is not available in this environment: test double");
        controller = new WebtuiPluginController(browserSessionService);
    }

    @Test
    void healthStaysOkAndReportsBrowserUnavailable() {
        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("plugin", "webtui");
        assertThat(response.getBody()).containsEntry("browserAvailable", false);
    }

    @Test
    void navigateReturnsCleanServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response =
                controller.navigate(new WebtuiPluginController.NavigateRequest("https://example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("browserAvailable", false);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void screenshotReturnsCleanJsonBodyNotAStackTrace() {
        ResponseEntity<byte[]> response = controller.screenshot();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"browserAvailable\":false");
        assertThat(body).doesNotContain("\tat ");
        assertThat(body).doesNotContain("StackTrace");
    }

    @Test
    void clickScrollTypeBackForwardAllReturn503WithCleanBody() {
        assertUnavailable(controller.click(new WebtuiPluginController.ClickRequest(1, 2)));
        assertUnavailable(controller.scroll(new WebtuiPluginController.ScrollRequest(10)));
        assertUnavailable(controller.type(new WebtuiPluginController.TypeRequest("hi")));
        assertUnavailable(controller.back());
        assertUnavailable(controller.forward());
    }

    private void assertUnavailable(ResponseEntity<Map<String, Object>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("browserAvailable", false);
        assertThat(response.getBody()).containsKey("error");
    }
}
