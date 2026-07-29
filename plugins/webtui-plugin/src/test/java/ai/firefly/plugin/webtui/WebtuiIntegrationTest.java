package ai.firefly.plugin.webtui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real {@link WebtuiPluginAutoConfiguration}, mirroring
 * {@code McpRegistryIntegrationTest}'s {@code @SpringBootTest} + {@code @Import} + {@code MockMvc} pattern.
 */
@SpringBootTest(classes = WebtuiIntegrationTest.TestApplication.class, properties = {
        "firefly.plugin.webtui.enabled=true",
        "firefly.plugin.webtui.homepage=about:blank"
})
class WebtuiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BrowserSessionService browserSessionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(WebtuiPluginAutoConfiguration.class)
    static class TestApplication {
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void healthReturns200WithBooleanBrowserAvailableRegardlessOfEnvironment() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/webtui/health"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("plugin").asText()).isEqualTo("webtui");
        assertThat(json.get("status").asText()).isEqualTo("ok");
        assertThat(json.has("browserAvailable")).isTrue();
        assertThat(json.get("browserAvailable").isBoolean()).isTrue();
    }

    @Test
    void navigateAndScreenshotProduceARealPngWhenBrowserIsAvailable() throws Exception {
        Assumptions.assumeTrue(browserSessionService.isAvailable(),
                "Headless Chromium is not available in this environment; skipping live browser test");

        mockMvc.perform(post("/api/webtui/navigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"about:blank\"}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/webtui/screenshot"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] png = result.getResponse().getContentAsByteArray();
        assertThat(png).isNotEmpty();
        assertThat(png.length).isGreaterThan(8);

        // PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
        assertThat(png[4]).isEqualTo((byte) 0x0D);
        assertThat(png[5]).isEqualTo((byte) 0x0A);
        assertThat(png[6]).isEqualTo((byte) 0x1A);
        assertThat(png[7]).isEqualTo((byte) 0x0A);
    }

    @Test
    void clickScrollTypeBackForwardWorkEndToEndWhenBrowserIsAvailable() throws Exception {
        Assumptions.assumeTrue(browserSessionService.isAvailable(),
                "Headless Chromium is not available in this environment; skipping live browser test");

        mockMvc.perform(post("/api/webtui/navigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"about:blank\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/webtui/scroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deltaY\":100}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/webtui/click")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":10,\"y\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/webtui/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isOk());

        // about:blank has no history, so back/forward should still respond cleanly (not throw a 500).
        mockMvc.perform(post("/api/webtui/back")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isLessThan(500));

        mockMvc.perform(post("/api/webtui/forward")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isLessThan(500));
    }
}
