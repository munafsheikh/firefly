package ai.firefly.plugin.cli;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.picocli.TuiCommand;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(name = "firefly", mixinStandardHelpOptions = true, description = "Firefly TUI application")
public class FireflyCommand extends TuiCommand {

    private enum Panel { LEFT, CENTER, RIGHT }

    private enum ToolbarAction { RUN, BUILD, TEST, SEARCH }

    private record MenuItemDef(String label, Runnable action) {}

    private record MenuDef(String label, List<MenuItemDef> items) {}

    private static final Map<String, String> EXPLORER_ITEMS = new LinkedHashMap<>();
    static {
        EXPLORER_ITEMS.put("firefly-core", "Core Spring Boot application.\n\nServes the dashboard, web terminal,\nand Swagger UI. Always loaded —\nno plugin jar required.");
        EXPLORER_ITEMS.put("actuator-plugin", "Exposes Spring Boot Actuator\nendpoints for health, metrics,\nand application info.\n\nVisible in Swagger UI only while\nthe plugin jar is present.");
        EXPLORER_ITEMS.put("ado-plugin", "Connects Firefly to Azure DevOps\nto retrieve, create and update\nwork items via REST API.");
        EXPLORER_ITEMS.put("cli-plugin", "Standalone TamboUI terminal\napplication for Firefly.\n\nYou are looking at it right now.");
    }
    private static final List<String> EXPLORER_KEYS = List.copyOf(EXPLORER_ITEMS.keySet());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_OUTPUT_LINES = 200;
    private static final int OUTPUT_PANEL_HEIGHT = 8;

    private Panel focus = Panel.LEFT;
    private final ListState explorerState = new ListState();
    private final ListState statusState = new ListState();
    private List<String> statusItems;

    private List<MenuDef> menus;
    private int openMenu = -1;
    private final ListState menuListState = new ListState();
    private List<Rect> menuLabelRects = List.of();
    private List<Rect> menuItemRects = List.of();

    private final Map<ToolbarAction, String> toolbarButtons = new LinkedHashMap<>();
    private Map<ToolbarAction, Rect> toolbarRects = Map.of();

    private boolean searchMode = false;
    private final TextInputState searchInput = new TextInputState();
    private List<String> filteredExplorerKeys = EXPLORER_KEYS;

    private boolean explorerVisible = true;
    private boolean statusVisible = true;
    private boolean darkTheme = true;

    private final Deque<String> outputLines = new ArrayDeque<>();

    private Rect leftPanelRect = Rect.ZERO;
    private Rect centerPanelRect = Rect.ZERO;
    private Rect rightPanelRect = Rect.ZERO;
    private Rect explorerListRect = Rect.ZERO;
    private Rect statusListRect = Rect.ZERO;

    @Override
    protected TuiConfig createConfig() {
        return TuiConfig.builder().mouseCapture(true).build();
    }

    @Override
    protected void runTui(TuiRunner runner) throws Exception {
        explorerState.select(0);
        statusItems = List.of(
                "Java: " + System.getProperty("java.version"),
                "OS: " + System.getProperty("os.name"),
                "User: " + System.getProperty("user.name"),
                "PID: " + ProcessHandle.current().pid()
        );
        statusState.select(0);

        toolbarButtons.put(ToolbarAction.RUN, "▶ Run");
        toolbarButtons.put(ToolbarAction.BUILD, "⚙ Build");
        toolbarButtons.put(ToolbarAction.TEST, "⚡ Test");
        toolbarButtons.put(ToolbarAction.SEARCH, "🔍 Search");

        menus = List.of(
                new MenuDef("File", List.of(
                        new MenuItemDef("About", () -> log("Firefly CLI v1.0.0 — TamboUI demo application")),
                        new MenuItemDef("Quit", runner::quit)
                )),
                new MenuDef("Edit", List.of(
                        new MenuItemDef("Toggle Theme", this::toggleTheme)
                )),
                new MenuDef("View", List.of(
                        new MenuItemDef("Toggle Explorer", () -> explorerVisible = !explorerVisible),
                        new MenuItemDef("Toggle Status Panel", () -> statusVisible = !statusVisible)
                )),
                new MenuDef("Terminal", List.of(
                        new MenuItemDef("Clear Output", outputLines::clear)
                )),
                new MenuDef("Help", List.of(
                        new MenuItemDef("About", () -> log("Firefly CLI v1.0.0 — TamboUI demo application"))
                ))
        );

        log("Firefly CLI ready. Mouse and keyboard are both live.");
        runner.run(this::handleEvent, this::render);
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        log("Theme switched to " + (darkTheme ? "dark" : "light"));
    }

    private Color accentColor() {
        return darkTheme ? Color.CYAN : Color.YELLOW;
    }

    private Color statusBarColor() {
        return darkTheme ? Color.BLUE : Color.MAGENTA;
    }

    private void log(String message) {
        outputLines.addLast("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message);
        while (outputLines.size() > MAX_OUTPUT_LINES) {
            outputLines.removeFirst();
        }
    }

    private void runToolbarAction(ToolbarAction action) {
        String selected = currentExplorerSelection();
        switch (action) {
            case RUN -> log("[RUN] Executing " + selected + "... done.");
            case BUILD -> log("[BUILD] Compiling " + selected + "... success.");
            case TEST -> log("[TEST] " + selected + ": 3 passed, 0 failed.");
            case SEARCH -> toggleSearchMode();
        }
    }

    private void toggleSearchMode() {
        searchMode = !searchMode;
        focus = Panel.LEFT;
        if (!searchMode) {
            searchInput.clear();
            recomputeFilteredExplorerKeys();
        } else {
            log("Search: type to filter EXPLORER, Enter to keep, Esc to cancel.");
        }
    }

    private void recomputeFilteredExplorerKeys() {
        String query = searchInput.text().toLowerCase();
        if (query.isBlank()) {
            filteredExplorerKeys = EXPLORER_KEYS;
        } else {
            filteredExplorerKeys = EXPLORER_KEYS.stream()
                    .filter(key -> key.toLowerCase().contains(query))
                    .toList();
        }
        if (filteredExplorerKeys.isEmpty()) {
            explorerState.select(null);
        } else if (explorerState.selected() == null || explorerState.selected() >= filteredExplorerKeys.size()) {
            explorerState.select(0);
        }
    }

    private String currentExplorerSelection() {
        Integer selected = explorerState.selected();
        if (selected == null || filteredExplorerKeys.isEmpty()) {
            return "(nothing selected)";
        }
        return filteredExplorerKeys.get(selected);
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (event instanceof KeyEvent key) {
            return handleKey(key, runner);
        }
        if (event instanceof MouseEvent mouse) {
            return handleMouse(mouse);
        }
        return false;
    }

    private boolean handleKey(KeyEvent key, TuiRunner runner) {
        if (searchMode) {
            return handleSearchKey(key);
        }
        if (openMenu >= 0) {
            return handleMenuKey(key);
        }
        if (key.isQuit()) {
            runner.quit();
            return false;
        }
        if (key.hasAlt() && key.code() == dev.tamboui.tui.event.KeyCode.CHAR) {
            for (int i = 0; i < menus.size(); i++) {
                if (Character.toLowerCase(key.character()) == Character.toLowerCase(menus.get(i).label().charAt(0))) {
                    openMenuAt(i);
                    return true;
                }
            }
        }
        if (key.isChar('/') && focus == Panel.LEFT) {
            toggleSearchMode();
            return true;
        }
        if (key.isFocusNext()) {
            focus = next(focus);
            return true;
        }
        if (key.isFocusPrevious()) {
            focus = previous(focus);
            return true;
        }
        if (focus != Panel.CENTER) {
            if (key.isUp()) {
                activeListState().selectPrevious();
                return true;
            }
            if (key.isDown()) {
                activeListState().selectNext(activeListSize() - 1);
                return true;
            }
        }
        return false;
    }

    private boolean handleSearchKey(KeyEvent key) {
        if (key.isKey(dev.tamboui.tui.event.KeyCode.ESCAPE)) {
            searchMode = false;
            searchInput.clear();
            recomputeFilteredExplorerKeys();
            return true;
        }
        if (key.isKey(dev.tamboui.tui.event.KeyCode.ENTER)) {
            searchMode = false;
            return true;
        }
        if (key.isDeleteBackward()) {
            searchInput.deleteBackward();
            recomputeFilteredExplorerKeys();
            return true;
        }
        if (key.isUp()) {
            explorerState.selectPrevious();
            return true;
        }
        if (key.isDown()) {
            explorerState.selectNext(filteredExplorerKeys.size() - 1);
            return true;
        }
        if (key.code() == dev.tamboui.tui.event.KeyCode.CHAR) {
            searchInput.insert(key.character());
            recomputeFilteredExplorerKeys();
            return true;
        }
        return false;
    }

    private boolean handleMenuKey(KeyEvent key) {
        List<MenuItemDef> items = menus.get(openMenu).items();
        if (key.isKey(dev.tamboui.tui.event.KeyCode.ESCAPE)) {
            closeMenu();
            return true;
        }
        if (key.isUp()) {
            menuListState.selectPrevious();
            return true;
        }
        if (key.isDown()) {
            menuListState.selectNext(items.size() - 1);
            return true;
        }
        if (key.isLeft()) {
            openMenuAt(Math.floorMod(openMenu - 1, menus.size()));
            return true;
        }
        if (key.isRight()) {
            openMenuAt(Math.floorMod(openMenu + 1, menus.size()));
            return true;
        }
        if (key.isKey(dev.tamboui.tui.event.KeyCode.ENTER)) {
            Integer selected = menuListState.selected();
            if (selected != null) {
                items.get(selected).action().run();
            }
            closeMenu();
            return true;
        }
        return false;
    }

    private boolean handleMouse(MouseEvent mouse) {
        if (mouse.kind() == MouseEventKind.PRESS && mouse.button() == MouseButton.LEFT) {
            if (openMenu >= 0) {
                for (int i = 0; i < menuItemRects.size(); i++) {
                    if (menuItemRects.get(i).contains(mouse.x(), mouse.y())) {
                        menus.get(openMenu).items().get(i).action().run();
                        closeMenu();
                        return true;
                    }
                }
                for (int i = 0; i < menuLabelRects.size(); i++) {
                    if (menuLabelRects.get(i).contains(mouse.x(), mouse.y())) {
                        openMenuAt(i == openMenu ? -1 : i);
                        return true;
                    }
                }
                closeMenu();
                return true;
            }
            for (int i = 0; i < menuLabelRects.size(); i++) {
                if (menuLabelRects.get(i).contains(mouse.x(), mouse.y())) {
                    openMenuAt(i);
                    return true;
                }
            }
            for (Map.Entry<ToolbarAction, Rect> entry : toolbarRects.entrySet()) {
                if (entry.getValue().contains(mouse.x(), mouse.y())) {
                    runToolbarAction(entry.getKey());
                    return true;
                }
            }
            if (explorerVisible && leftPanelRect.contains(mouse.x(), mouse.y())) {
                focus = Panel.LEFT;
                if (explorerListRect.contains(mouse.x(), mouse.y())) {
                    int clicked = explorerState.offset() + (mouse.y() - explorerListRect.y());
                    if (clicked >= 0 && clicked < filteredExplorerKeys.size()) {
                        explorerState.select(clicked);
                    }
                }
                return true;
            }
            if (centerPanelRect.contains(mouse.x(), mouse.y())) {
                focus = Panel.CENTER;
                return true;
            }
            if (statusVisible && rightPanelRect.contains(mouse.x(), mouse.y())) {
                focus = Panel.RIGHT;
                if (statusListRect.contains(mouse.x(), mouse.y())) {
                    int clicked = statusState.offset() + (mouse.y() - statusListRect.y());
                    if (clicked >= 0 && clicked < statusItems.size()) {
                        statusState.select(clicked);
                    }
                }
                return true;
            }
            if (openMenu >= 0) {
                closeMenu();
                return true;
            }
        }
        if (mouse.kind() == MouseEventKind.SCROLL_UP || mouse.kind() == MouseEventKind.SCROLL_DOWN) {
            boolean up = mouse.kind() == MouseEventKind.SCROLL_UP;
            if (explorerVisible && explorerListRect.contains(mouse.x(), mouse.y())) {
                if (up) explorerState.selectPrevious(); else explorerState.selectNext(filteredExplorerKeys.size() - 1);
                return true;
            }
            if (statusVisible && statusListRect.contains(mouse.x(), mouse.y())) {
                if (up) statusState.selectPrevious(); else statusState.selectNext(statusItems.size() - 1);
                return true;
            }
        }
        return false;
    }

    private void openMenuAt(int index) {
        openMenu = index;
        if (index >= 0) {
            menuListState.select(0);
        }
    }

    private void closeMenu() {
        openMenu = -1;
    }

    private Panel next(Panel panel) {
        return switch (panel) {
            case LEFT -> Panel.CENTER;
            case CENTER -> Panel.RIGHT;
            case RIGHT -> Panel.LEFT;
        };
    }

    private Panel previous(Panel panel) {
        return switch (panel) {
            case LEFT -> Panel.RIGHT;
            case CENTER -> Panel.LEFT;
            case RIGHT -> Panel.CENTER;
        };
    }

    private ListState activeListState() {
        return focus == Panel.RIGHT ? statusState : explorerState;
    }

    private int activeListSize() {
        return focus == Panel.RIGHT ? statusItems.size() : filteredExplorerKeys.size();
    }

    private void render(Frame frame) {
        List<Rect> rows = Layout.vertical()
                .constraints(Constraint.length(1), Constraint.length(1), Constraint.fill(1), Constraint.length(1))
                .split(frame.area());
        Rect menuBarArea = rows.get(0);
        Rect toolBarArea = rows.get(1);
        Rect bodyArea = rows.get(2);
        Rect statusBarArea = rows.get(3);

        List<Constraint> colConstraints = new ArrayList<>();
        if (explorerVisible) colConstraints.add(Constraint.length(26));
        colConstraints.add(Constraint.fill(1));
        if (statusVisible) colConstraints.add(Constraint.length(28));
        List<Rect> columns = Layout.horizontal().constraints(colConstraints).split(bodyArea);

        int idx = 0;
        Rect leftArea = explorerVisible ? columns.get(idx++) : Rect.ZERO;
        Rect centerArea = columns.get(idx++);
        Rect rightArea = statusVisible ? columns.get(idx) : Rect.ZERO;

        leftPanelRect = leftArea;
        centerPanelRect = centerArea;
        rightPanelRect = rightArea;

        renderMenuBar(frame, menuBarArea);
        renderToolBar(frame, toolBarArea);
        if (explorerVisible) {
            renderExplorer(frame, leftArea);
        }
        renderCenter(frame, centerArea);
        if (statusVisible) {
            renderStatusPanel(frame, rightArea);
        }
        renderStatusBar(frame, statusBarArea);

        if (openMenu >= 0) {
            renderMenuDropdown(frame, frame.area().width());
        }
    }

    private void renderMenuBar(Frame frame, Rect area) {
        Style base = Style.create().bg(Color.DARK_GRAY).fg(Color.WHITE);
        Style active = Style.create().bg(accentColor()).fg(Color.BLACK).bold();
        List<Span> spans = new ArrayList<>();
        List<Rect> rects = new ArrayList<>();
        int x = area.x();
        for (int i = 0; i < menus.size(); i++) {
            String label = " " + menus.get(i).label() + " ";
            spans.add(Span.styled(label, i == openMenu ? active : base));
            rects.add(new Rect(x, area.y(), label.length(), 1));
            x += label.length();
        }
        spans.add(Span.styled(" ".repeat(Math.max(0, area.width() - (x - area.x()))), base));
        menuLabelRects = rects;
        Paragraph menuBar = Paragraph.builder().text(Text.from(Line.from(spans))).background(Color.DARK_GRAY).build();
        frame.renderWidget(menuBar, area);
    }

    private void renderToolBar(Frame frame, Rect area) {
        Style base = Style.create().bg(Color.GRAY).fg(Color.BLACK);
        Style active = Style.create().bg(accentColor()).fg(Color.BLACK).bold();
        List<Span> spans = new ArrayList<>();
        Map<ToolbarAction, Rect> rects = new LinkedHashMap<>();
        int x = area.x();
        for (Map.Entry<ToolbarAction, String> entry : toolbarButtons.entrySet()) {
            String label = " " + entry.getValue() + " ";
            boolean isActive = entry.getKey() == ToolbarAction.SEARCH && searchMode;
            spans.add(Span.styled(label, isActive ? active : base));
            rects.put(entry.getKey(), new Rect(x, area.y(), label.length(), 1));
            x += label.length();
        }
        spans.add(Span.styled(" ".repeat(Math.max(0, area.width() - (x - area.x()))), base));
        toolbarRects = rects;
        Paragraph toolBar = Paragraph.builder().text(Text.from(Line.from(spans))).background(Color.GRAY).build();
        frame.renderWidget(toolBar, area);
    }

    private void renderExplorer(Frame frame, Rect area) {
        boolean focused = focus == Panel.LEFT;
        Rect searchRow = Rect.ZERO;
        Rect explorerArea = area;
        if (searchMode) {
            List<Rect> split = Layout.vertical().constraints(Constraint.length(1), Constraint.fill(1)).split(area);
            searchRow = split.get(0);
            explorerArea = split.get(1);
            TextInput input = TextInput.builder()
                    .placeholder("filter…")
                    .foreground(Color.WHITE)
                    .background(Color.DARK_GRAY)
                    .build();
            frame.renderStatefulWidget(input, searchRow, searchInput);
        }

        Block block = Block.builder()
                .title(searchMode ? "EXPLORER (filtering)" : "EXPLORER")
                .borders(Borders.ALL)
                .borderColor(focused ? accentColor() : Color.GRAY)
                .build();
        frame.renderWidget(block, explorerArea);

        explorerListRect = block.inner(explorerArea);
        ListWidget list = ListWidget.builder()
                .items(filteredExplorerKeys.stream().map(ListItem::from).toList())
                .highlightStyle(Style.create().bg(accentColor()).fg(Color.BLACK).bold())
                .highlightSymbol("▸ ")
                .build();
        frame.renderStatefulWidget(list, explorerListRect, explorerState);
    }

    private void renderCenter(Frame frame, Rect area) {
        List<Rect> split = Layout.vertical()
                .constraints(Constraint.fill(1), Constraint.length(OUTPUT_PANEL_HEIGHT))
                .split(area);
        renderPreview(frame, split.get(0));
        renderOutput(frame, split.get(1));
    }

    private void renderPreview(Frame frame, Rect area) {
        boolean focused = focus == Panel.CENTER;
        String key = filteredExplorerKeys.isEmpty() ? "(no match)" : currentExplorerSelection();
        Block block = Block.builder()
                .title(key)
                .borders(Borders.ALL)
                .borderColor(focused ? accentColor() : Color.GRAY)
                .build();
        frame.renderWidget(block, area);

        String content = EXPLORER_ITEMS.getOrDefault(key, "No preview available.");
        Paragraph preview = Paragraph.builder().text(Text.from(content)).build();
        frame.renderWidget(preview, block.inner(area));
    }

    private void renderOutput(Frame frame, Rect area) {
        Block block = Block.builder()
                .title("OUTPUT")
                .borders(Borders.ALL)
                .borderColor(Color.GRAY)
                .build();
        frame.renderWidget(block, area);

        Rect inner = block.inner(area);
        List<String> lines = outputLines.stream().toList();
        int start = Math.max(0, lines.size() - inner.height());
        String content = String.join("\n", lines.subList(start, lines.size()));
        Paragraph output = Paragraph.builder().text(Text.from(content)).foreground(Color.GREEN).build();
        frame.renderWidget(output, inner);
    }

    private void renderStatusPanel(Frame frame, Rect area) {
        boolean focused = focus == Panel.RIGHT;
        Block block = Block.builder()
                .title("STATUS")
                .borders(Borders.ALL)
                .borderColor(focused ? accentColor() : Color.GRAY)
                .build();
        frame.renderWidget(block, area);

        statusListRect = block.inner(area);
        ListWidget list = ListWidget.builder()
                .items(statusItems.stream().map(ListItem::from).toList())
                .highlightStyle(Style.create().bg(accentColor()).fg(Color.BLACK).bold())
                .highlightSymbol("▸ ")
                .build();
        frame.renderStatefulWidget(list, statusListRect, statusState);
    }

    private void renderStatusBar(Frame frame, Rect area) {
        Style base = Style.create().bg(statusBarColor()).fg(Color.WHITE);
        String hint = searchMode
                ? " SEARCH: " + searchInput.text() + "  |  Enter: keep  Esc: cancel "
                : " FOCUS: " + focus + "  |  Tab: switch panel  ↑↓: navigate  Alt+letter: menu  /: search  Click: use mouse  q: quit ";
        Paragraph statusBar = Paragraph.builder()
                .text(Text.from(Line.from(Span.styled(hint, base))))
                .background(statusBarColor())
                .build();
        frame.renderWidget(statusBar, area);
    }

    private void renderMenuDropdown(Frame frame, int frameWidth) {
        MenuDef menu = menus.get(openMenu);
        int width = menu.items().stream().mapToInt(item -> item.label().length()).max().orElse(10) + 4;
        int height = menu.items().size() + 2;
        Rect anchor = menuLabelRects.get(openMenu);
        int x = Math.min(anchor.x(), Math.max(0, frameWidth - width));
        Rect dropdown = new Rect(x, 1, width, height);

        Block block = Block.builder()
                .borders(Borders.ALL)
                .borderColor(accentColor())
                .background(Color.BLACK)
                .build();
        frame.renderWidget(block, dropdown);

        Rect inner = block.inner(dropdown);
        List<Rect> itemRects = new ArrayList<>();
        for (int i = 0; i < menu.items().size(); i++) {
            itemRects.add(new Rect(inner.x(), inner.y() + i, inner.width(), 1));
        }
        menuItemRects = itemRects;

        ListWidget list = ListWidget.builder()
                .items(menu.items().stream().map(item -> ListItem.from(item.label())).toList())
                .highlightStyle(Style.create().bg(accentColor()).fg(Color.BLACK).bold())
                .highlightSymbol("▸ ")
                .background(Color.BLACK)
                .build();
        frame.renderStatefulWidget(list, inner, menuListState);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new FireflyCommand()).execute(args));
    }
}
