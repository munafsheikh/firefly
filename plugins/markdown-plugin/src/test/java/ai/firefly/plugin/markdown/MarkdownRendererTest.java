package ai.firefly.plugin.markdown;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersHeading() {
        String html = renderer.renderToHtml("# Title");

        assertThat(html.trim()).isEqualTo("<h1>Title</h1>");
    }

    @Test
    void rendersEmphasisAndLists() {
        String html = renderer.renderToHtml("**bold** and *italic*\n\n- one\n- two\n");

        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<em>italic</em>");
        assertThat(html).contains("<li>one</li>");
        assertThat(html).contains("<li>two</li>");
    }

    @Test
    void rendersGfmTables() {
        String markdown = "| A | B |\n| --- | --- |\n| 1 | 2 |\n";

        String html = renderer.renderToHtml(markdown);

        assertThat(html).contains("<table>");
        assertThat(html).contains("<td>1</td>");
    }

    @Test
    void handlesNullAndBlankInput() {
        assertThat(renderer.renderToHtml(null)).isNotNull();
        assertThat(renderer.renderToHtml("")).isNotNull();
    }
}
