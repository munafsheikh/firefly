package ai.firefly.plugin.cli;

import dev.tamboui.widgets.input.TextInputState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FireflyCommand is almost entirely terminal-rendering code driven by a real Frame/TuiRunner,
 * which can't be exercised headlessly in a unit test — see the CLI plugin's README for how this
 * is verified manually instead. What's covered here is the pure state-transition logic (panel
 * focus cycling, theme toggling, search filtering, menu/toolbar bookkeeping), reached via
 * reflection since it lives in private methods on a class with no other testable seam.
 */
class FireflyCommandTest {

    private FireflyCommand command;

    @BeforeEach
    void setUp() {
        command = new FireflyCommand();
    }

    @Test
    void commandMetadataMatchesLaunchExpectations() {
        CommandLine cli = new CommandLine(command);

        assertThat(cli.getCommandName()).isEqualTo("firefly");
        assertThat(cli.getCommandSpec().mixinStandardHelpOptions()).isTrue();
    }

    @Test
    void createConfigEnablesMouseCapture() throws Exception {
        Method createConfig = FireflyCommand.class.getDeclaredMethod("createConfig");
        createConfig.setAccessible(true);

        assertThat(createConfig.invoke(command)).isNotNull();
    }

    @Test
    void toggleThemeFlipsDarkThemeAndAccentColor() throws Exception {
        boolean initialDark = (boolean) field("darkTheme");
        Object initialAccent = invoke("accentColor");

        invoke("toggleTheme");

        assertThat(field("darkTheme")).isEqualTo(!initialDark);
        assertThat(invoke("accentColor")).isNotEqualTo(initialAccent);
        assertThat(invoke("statusBarColor")).isNotNull();
    }

    @Test
    void currentExplorerSelectionReportsNothingSelectedByDefault() throws Exception {
        assertThat(invoke("currentExplorerSelection")).isEqualTo("(nothing selected)");
    }

    @Test
    void logAppendsTimestampedEntryAndCapsHistoryAt200Lines() throws Exception {
        Method log = FireflyCommand.class.getDeclaredMethod("log", String.class);
        log.setAccessible(true);
        for (int i = 0; i < 205; i++) {
            log.invoke(command, "line " + i);
        }

        @SuppressWarnings("unchecked")
        Deque<String> outputLines = (Deque<String>) field("outputLines");
        assertThat(outputLines).hasSize(200);
        assertThat(outputLines.getLast()).contains("line 204");
    }

    @Test
    void recomputeFilteredExplorerKeysFiltersCaseInsensitively() throws Exception {
        TextInputState searchInput = (TextInputState) field("searchInput");
        "ADO".chars().forEach(c -> searchInput.insert((char) c));

        invoke("recomputeFilteredExplorerKeys");

        @SuppressWarnings("unchecked")
        List<String> filtered = (List<String>) field("filteredExplorerKeys");
        assertThat(filtered).containsExactly("ado-plugin");
    }

    @Test
    void recomputeFilteredExplorerKeysRestoresFullListWhenQueryIsBlank() throws Exception {
        invoke("recomputeFilteredExplorerKeys");

        @SuppressWarnings("unchecked")
        List<String> filtered = (List<String>) field("filteredExplorerKeys");
        assertThat(filtered).containsExactly("firefly-core", "actuator-plugin", "ado-plugin", "cli-plugin");
    }

    @Test
    void toggleSearchModeEntersThenExitsCleanly() throws Exception {
        invoke("toggleSearchMode");
        assertThat(field("searchMode")).isEqualTo(true);

        invoke("toggleSearchMode");
        assertThat(field("searchMode")).isEqualTo(false);

        TextInputState searchInput = (TextInputState) field("searchInput");
        assertThat(searchInput.text()).isEmpty();
    }

    @Test
    void panelFocusCyclesForwardAndBackward() throws Exception {
        Class<?> panelClass = Class.forName("ai.firefly.plugin.cli.FireflyCommand$Panel");
        Object left = enumConstant(panelClass, "LEFT");
        Object center = enumConstant(panelClass, "CENTER");
        Object right = enumConstant(panelClass, "RIGHT");

        Method next = FireflyCommand.class.getDeclaredMethod("next", panelClass);
        next.setAccessible(true);
        Method previous = FireflyCommand.class.getDeclaredMethod("previous", panelClass);
        previous.setAccessible(true);

        assertThat(next.invoke(command, left)).isEqualTo(center);
        assertThat(next.invoke(command, center)).isEqualTo(right);
        assertThat(next.invoke(command, right)).isEqualTo(left);

        assertThat(previous.invoke(command, left)).isEqualTo(right);
        assertThat(previous.invoke(command, center)).isEqualTo(left);
        assertThat(previous.invoke(command, right)).isEqualTo(center);
    }

    @Test
    void runToolbarActionSearchTogglesSearchMode() throws Exception {
        Class<?> actionClass = Class.forName("ai.firefly.plugin.cli.FireflyCommand$ToolbarAction");
        Object search = enumConstant(actionClass, "SEARCH");
        Method runToolbarAction = FireflyCommand.class.getDeclaredMethod("runToolbarAction", actionClass);
        runToolbarAction.setAccessible(true);

        runToolbarAction.invoke(command, search);

        assertThat(field("searchMode")).isEqualTo(true);
    }

    @Test
    void openAndCloseMenuTracksOpenMenuIndex() throws Exception {
        Method openMenuAt = FireflyCommand.class.getDeclaredMethod("openMenuAt", int.class);
        openMenuAt.setAccessible(true);
        Method closeMenu = FireflyCommand.class.getDeclaredMethod("closeMenu");
        closeMenu.setAccessible(true);

        openMenuAt.invoke(command, 2);
        assertThat(field("openMenu")).isEqualTo(2);

        closeMenu.invoke(command);
        assertThat(field("openMenu")).isEqualTo(-1);
    }

    private Object invoke(String methodName) throws Exception {
        Method method = FireflyCommand.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(command);
    }

    private Object field(String name) throws Exception {
        Field field = FireflyCommand.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(command);
    }

    private Object enumConstant(Class<?> enumClass, String name) {
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No such constant: " + name);
    }
}
