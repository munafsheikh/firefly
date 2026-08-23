package ai.firefly.plugin.bashrunner;

import java.util.List;

public record BashRunRequest(String script, List<String> args, Boolean open) {
    public List<String> safeArgs() {
        return args == null ? List.of() : List.copyOf(args);
    }

    public boolean shouldOpen() {
        return open == null || open;
    }
}
