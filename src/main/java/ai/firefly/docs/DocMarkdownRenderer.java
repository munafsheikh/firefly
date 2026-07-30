package ai.firefly.docs;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side Markdown-to-HTML rendering for the Documentation Browser, backed by flexmark-java
 * (CommonMark + GFM tables) — the same library {@code markdown-plugin} uses for its preview pane.
 *
 * <p>Doc authors reference screenshots with a plain relative path (e.g. {@code screenshots/dashboard.png}).
 * Since those images live inside a JAR (or the core classpath) rather than on a public static path,
 * this rewrites any relative {@code <img src="...">} to the section's {@code /docs/{sectionId}/assets/...}
 * endpoint after rendering.
 */
@Component
public class DocMarkdownRenderer {

    private static final Pattern RELATIVE_IMG_SRC = Pattern.compile("(<img\\s+[^>]*?src=\")(?!https?://|/)([^\"]+)(\")");

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public DocMarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    public String render(String sectionId, String markdown) {
        Node document = parser.parse(markdown == null ? "" : markdown);
        String html = htmlRenderer.render(document);
        String assetBase = "/docs/" + sectionId + "/assets/";
        Matcher matcher = RELATIVE_IMG_SRC.matcher(html);
        return matcher.replaceAll("$1" + Matcher.quoteReplacement(assetBase) + "$2$3");
    }
}
