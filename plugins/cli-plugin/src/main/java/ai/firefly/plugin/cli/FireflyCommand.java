package ai.firefly.plugin.cli;

import dev.tamboui.picocli.TuiCommand;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.text.Text;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "firefly", mixinStandardHelpOptions = true, description = "Firefly TUI application")
public class FireflyCommand extends TuiCommand {

    @Override
    protected void runTui(TuiRunner runner) throws Exception {
        runner.run(this::handleEvent, this::render);
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (event instanceof KeyEvent k && k.isQuit()) {
            runner.quit();
            return false;
        }
        return false;
    }

    private void render(Frame frame) {
        var paragraph = Paragraph.builder()
            .text(Text.from("Hello, Firefly! Press 'q' to quit."))
            .build();
        frame.renderWidget(paragraph, frame.area());
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new FireflyCommand()).execute(args));
    }
}
