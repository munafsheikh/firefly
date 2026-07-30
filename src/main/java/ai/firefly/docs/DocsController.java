package ai.firefly.docs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * The in-app Documentation Browser: renders core docs plus every installed plugin's bundled
 * {@code META-INF/firefly/docs/index.md} as one combined, navigable set of pages at {@code /docs}.
 */
@Controller
@RequestMapping("/docs")
@RequiredArgsConstructor
public class DocsController {

    private static final String INDEX_PAGE = "index.md";

    private final DocsManager docsManager;
    private final DocMarkdownRenderer renderer;

    @GetMapping({"", "/"})
    public String landing(Model model) {
        List<DocSection> sections = docsManager.listSections();
        String firstSectionId = sections.isEmpty() ? "overview" : sections.get(0).id();
        return renderPage(model, firstSectionId);
    }

    @GetMapping("/{sectionId}")
    public String section(@PathVariable String sectionId, Model model) {
        return renderPage(model, sectionId);
    }

    @GetMapping("/{sectionId}/assets/{*assetPath}")
    public ResponseEntity<byte[]> asset(@PathVariable String sectionId, @PathVariable String assetPath) {
        String cleaned = assetPath.startsWith("/") ? assetPath.substring(1) : assetPath;
        return docsManager.readAsset(sectionId, cleaned)
            .map(asset -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .body(asset.bytes()))
            .orElse(ResponseEntity.notFound().build());
    }

    private String renderPage(Model model, String sectionId) {
        List<DocSection> sections = docsManager.listSections();
        model.addAttribute("sections", sections);
        model.addAttribute("activeSectionId", sectionId);

        String markdown = docsManager.readMarkdown(sectionId, INDEX_PAGE)
            .orElse("# No documentation found\n\nNo documentation is bundled for `" + sectionId + "`.");
        model.addAttribute("contentHtml", renderer.render(sectionId, markdown));

        return "docs";
    }
}
