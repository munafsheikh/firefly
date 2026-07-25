package ai.firefly.dashboard;

import ai.firefly.testdoc.DocScreenshot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots a real embedded server so {@code ScreenshotDocumentationExtension} has something to
 * screenshot — demonstrates the doc-screenshot system end to end against the dashboard and
 * Swagger UI pages.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardScreenshotTests {

    @LocalServerPort
    private int port;

    @Test
    @DocScreenshot(paths = {"/"})
    void dashboardRenders() {
        String body = RestClient.create()
                .get()
                .uri("http://localhost:{port}/", port)
                .retrieve()
                .body(String.class);

        assertTrue(body.contains("Firefly"));
    }

    @Test
    @DocScreenshot(paths = {"/swagger-ui/index.html"})
    void swaggerUiRenders() {
        boolean success = RestClient.create()
                .get()
                .uri("http://localhost:{port}/swagger-ui/index.html", port)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .is2xxSuccessful();

        assertTrue(success);
    }

    @Test
    @DocScreenshot(paths = {"/terminal"})
    void terminalPageRenders() {
        // The terminal forwards to the static terminal.html page with xterm.js.
        // In the test environment no CLI plugin JAR is present, so the WebSocket PTY
        // connection will not establish — but the terminal shell/container still renders.
        boolean success = RestClient.create()
                .get()
                .uri("http://localhost:{port}/terminal", port)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .is2xxSuccessful();

        assertTrue(success);
    }
}
