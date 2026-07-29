package ai.firefly.plugin.plantuml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PlantumlIntegrationTest.TestApplication.class, properties = "firefly.plugin.plantuml.enabled=true")
class PlantumlIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PlantumlPluginAutoConfiguration.class)
    static class TestApplication {
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void healthReturnsOkStatus() throws Exception {
        mockMvc.perform(get("/api/plantuml/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plugin").value("plantuml"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void rendersValidSourceToSvg() throws Exception {
        String source = "@startuml\nAlice -> Bob: hello\n@enduml";

        mockMvc.perform(post("/api/plantuml/render")
                        .param("format", "svg")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(source))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/svg+xml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
    }

    @Test
    void defaultsToSvgWhenFormatOmitted() throws Exception {
        String source = "@startuml\nAlice -> Bob: hello\n@enduml";

        mockMvc.perform(post("/api/plantuml/render")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(source))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/svg+xml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
    }

    @Test
    void rendersValidSourceToPng() throws Exception {
        String source = "@startuml\nAlice -> Bob: hello\n@enduml";

        mockMvc.perform(post("/api/plantuml/render")
                        .param("format", "png")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(source))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/png")));
    }

    @Test
    void invalidSourceReturnsBadRequestNotServerError() throws Exception {
        String garbage = "this is not plantuml at all !!! ###";

        mockMvc.perform(post("/api/plantuml/render")
                        .param("format", "svg")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(garbage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void syntaxErrorInsideValidBlockReturnsBadRequest() throws Exception {
        String badSyntax = "@startuml\nfoo bar totally bogus command !!\n@enduml";

        mockMvc.perform(post("/api/plantuml/render")
                        .param("format", "svg")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(badSyntax))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void emptySourceReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/plantuml/render")
                        .param("format", "svg")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sourceExceedingMaxLengthReturnsBadRequest() throws Exception {
        String tooLong = "@startuml\n" + "x".repeat(21000) + "\n@enduml";

        mockMvc.perform(post("/api/plantuml/render")
                        .param("format", "svg")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(tooLong))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void pageIsServed() throws Exception {
        mockMvc.perform(get("/pages/plantuml"))
                .andExpect(status().isOk());
    }
}
