package ai.firefly.plugin.bashrunner;

public record BashRunResult(
        String script,
        int exitCode,
        boolean timedOut,
        long durationMs,
        String stdout,
        String stderr,
        String openTarget,
        boolean openAttempted,
        boolean openStarted,
        String openError) {
}
