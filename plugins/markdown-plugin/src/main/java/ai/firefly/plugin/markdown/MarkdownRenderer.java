package ai.firefly.plugin.markdown;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.List;

/**
 * Server-side Markdown (CommonMark + GFM tables) to HTML renderer, backed by flexmark-java.
 */
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public MarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    public String renderToHtml(String markdown) {
        Node document = parser.parse(markdown == null ? "" : markdown);
        return htmlRenderer.render(document);
    }
}
