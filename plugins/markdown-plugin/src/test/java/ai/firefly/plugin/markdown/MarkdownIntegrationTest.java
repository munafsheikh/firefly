package ai.firefly.plugin.markdown;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MarkdownIntegrationTest.TestApplication.class, properties = "firefly.plugin.markdown.enabled=true")
class MarkdownIntegrationTest {

    private static final Path ROOT_DIR = createRootDir();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(MarkdownPluginAutoConfiguration.class)
    static class TestApplication {
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("firefly.plugin.markdown.root-dir", ROOT_DIR::toString);
    }

    @Test
    void healthEndpointReportsPluginStatus() throws Exception {
        mockMvc.perform(get("/api/markdown/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plugin").value("markdown"))
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void createReadUpdateAndDeleteAFile() throws Exception {
        mockMvc.perform(put("/api/markdown/files/notes/hello.md")
                .contentType(MediaType.TEXT_PLAIN)
                .content("# Hello\n"))
            .andExpect(status().isOk());

        assertThat(Files.exists(ROOT_DIR.resolve("notes/hello.md"))).isTrue();

        mockMvc.perform(get("/api/markdown/files/notes/hello.md"))
            .andExpect(status().isOk())
            .andExpect(content().string("# Hello\n"));

        mockMvc.perform(put("/api/markdown/files/notes/hello.md")
                .contentType(MediaType.TEXT_PLAIN)
                .content("# Hello Again\n"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/markdown/files/notes/hello.md"))
            .andExpect(status().isOk())
            .andExpect(content().string("# Hello Again\n"));

        mockMvc.perform(delete("/api/markdown/files/notes/hello.md"))
            .andExpect(status().isNoContent());

        assertThat(Files.exists(ROOT_DIR.resolve("notes/hello.md"))).isFalse();

        mockMvc.perform(get("/api/markdown/files/notes/hello.md"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listsCreatedMarkdownFiles() throws Exception {
        mockMvc.perform(put("/api/markdown/files/list-me.md")
                .contentType(MediaType.TEXT_PLAIN)
                .content("content"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/markdown/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.path == 'list-me.md')]").exists());
    }

    @Test
    void previewEndpointRendersActualHtml() throws Exception {
        mockMvc.perform(post("/api/markdown/preview")
                .contentType(MediaType.TEXT_PLAIN)
                .content("# Title\n"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<h1>Title</h1>")));
    }

    @Test
    void rejectsPathTraversalAttemptOnRead() throws Exception {
        mockMvc.perform(get("/api/markdown/files/../../../../etc/passwd"))
            .andExpect(status().is4xxClientError())
            .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain("root:"));
    }

    @Test
    void rejectsPathTraversalAttemptOnWrite() throws Exception {
        mockMvc.perform(put("/api/markdown/files/../../../../tmp/pwned.md")
                .contentType(MediaType.TEXT_PLAIN)
                .content("pwned"))
            .andExpect(status().is4xxClientError());

        assertThat(Files.exists(Path.of("/tmp/pwned.md"))).isFalse();
    }

    @Test
    void rejectsPathTraversalAttemptOnDelete() throws Exception {
        mockMvc.perform(delete("/api/markdown/files/../../../../etc/passwd"))
            .andExpect(status().is4xxClientError());
    }

    private static Path createRootDir() {
        try {
            return Files.createTempDirectory("markdown-plugin-integration-");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create temp directory", e);
        }
    }
}
