package ai.firefly.plugin.bashrunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XdgOpenService {

    private final BashRunnerProperties properties;

    public XdgOpenService(BashRunnerProperties properties) {
        this.properties = properties;
    }

    public OpenResult open(String target) {
        if (!properties.isXdgOpenEnabled()) {
            return new OpenResult(false, "xdg-open is disabled by configuration");
        }
        try {
            new ProcessBuilder(properties.getXdgOpenCommand(), target)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return new OpenResult(true, null);
        } catch (IOException e) {
            return new OpenResult(false, e.getMessage());
        }
    }

    public boolean isAvailable() {
        String command = properties.getXdgOpenCommand();
        if (command == null || command.isBlank()) {
            return false;
        }
        if (command.contains("/")) {
            return Files.isExecutable(Path.of(command));
        }
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (!dir.isBlank() && Files.isExecutable(Path.of(dir).resolve(command))) {
                return true;
            }
        }
        return false;
    }

    public record OpenResult(boolean started, String error) {}
}
