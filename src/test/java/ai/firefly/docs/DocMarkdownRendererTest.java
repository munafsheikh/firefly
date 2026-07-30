package ai.firefly.docs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocMarkdownRendererTest {

    private final DocMarkdownRenderer renderer = new DocMarkdownRenderer();

    @Test
    void rendersHeadingsAndTables() {
        String html = renderer.render("overview", "# Title\n\n| A | B |\n|---|---|\n| 1 | 2 |\n");

        assertThat(html).contains("<h1>Title</h1>");
        assertThat(html).contains("<table>");
    }

    @Test
    void rewritesRelativeImageSrcToSectionAssetsEndpoint() {
        String html = renderer.render("plantuml", "![Preview](screenshots/preview.png)");

        assertThat(html).contains("src=\"/docs/plantuml/assets/screenshots/preview.png\"");
    }

    @Test
    void leavesAbsoluteAndExternalImageSrcAlone() {
        String html = renderer.render("overview", "![Ext](https://example.com/a.png)\n\n![Root](/static/b.png)");

        assertThat(html).contains("src=\"https://example.com/a.png\"");
        assertThat(html).contains("src=\"/static/b.png\"");
    }
}
