package ai.firefly.plugin.bashrunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class BashRunnerService {

    private final BashRunnerProperties properties;
    private final OpenTargetResolver targetResolver;
    private final XdgOpenService xdgOpenService;

    public BashRunnerService(BashRunnerProperties properties, OpenTargetResolver targetResolver, XdgOpenService xdgOpenService) {
        this.properties = properties;
        this.targetResolver = targetResolver;
        this.xdgOpenService = xdgOpenService;
    }

    public Path getScriptsRoot() throws IOException {
        return ensureRoot();
    }

    public List<ScriptInfo> listScripts() throws IOException {
        Path root = ensureRoot();
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".sh"))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .map(path -> new ScriptInfo(
                            root.relativize(path).toString().replace(java.io.File.separatorChar, '/'),
                            sizeQuietly(path),
                            Files.isExecutable(path)))
                    .toList();
        }
    }

    public BashRunResult run(BashRunRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        String requestedScript = Objects.requireNonNull(request.script(), "script").strip();
        if (requestedScript.isEmpty()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        if (!requestedScript.endsWith(".sh")) {
            throw new IllegalArgumentException("only .sh scripts may be executed");
        }

        List<String> args = request.safeArgs();
        for (String arg : args) {
            if (arg == null || arg.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("arguments must not contain null values or NUL characters");
            }
        }

        Path root = ensureRoot();
        Path candidate = root.resolve(requestedScript).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("script path escapes the configured scripts root");
        }
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("script does not exist: " + requestedScript);
        }

        Path script = candidate.toRealPath();
        if (!script.startsWith(root)) {
            throw new IllegalArgumentException("script symlink escapes the configured scripts root");
        }
        if (!Files.isRegularFile(script)) {
            throw new IllegalArgumentException("script is not a regular file: " + requestedScript);
        }

        List<String> command = new ArrayList<>(2 + args.size());
        command.add(properties.getBashCommand());
        command.add(script.toString());
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Path workingDirectory = script.getParent();
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.environment().put("FIREFLY_SCRIPT_ROOT", root.toString());
        processBuilder.environment().put("FIREFLY_SCRIPT", requestedScript);

        long startedAt = System.nanoTime();
        Process process = processBuilder.start();
        ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        Future<CapturedOutput> stdoutFuture = ioExecutor.submit(() -> capture(process.getInputStream()));
        Future<CapturedOutput> stderrFuture = ioExecutor.submit(() -> capture(process.getErrorStream()));

        boolean timedOut;
        int exitCode;
        try {
            Duration timeout = properties.getTimeout();
            long timeoutMs = Math.max(1L, timeout == null ? Duration.ofMinutes(5).toMillis() : timeout.toMillis());
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            timedOut = !finished;
            if (timedOut) {
                terminateProcessTree(process);
                process.waitFor(1, TimeUnit.SECONDS);
            }
            exitCode = process.isAlive() ? -1 : process.exitValue();
        } finally {
            if (process.isAlive()) {
                terminateProcessTree(process);
            }
        }

        CapturedOutput stdout = awaitOutput(stdoutFuture, "stdout");
        CapturedOutput stderr = awaitOutput(stderrFuture, "stderr");
        ioExecutor.shutdownNow();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        Optional<String> target = exitCode == 0 && !timedOut
                ? targetResolver.resolve(stdout.text(), workingDirectory)
                : Optional.empty();

        boolean openAttempted = false;
        boolean openStarted = false;
        String openError = null;
        if (exitCode == 0 && !timedOut && request.shouldOpen()) {
            if (target.isPresent()) {
                openAttempted = true;
                XdgOpenService.OpenResult result = xdgOpenService.open(target.get());
                openStarted = result.started();
                openError = result.error();
            } else {
                openError = "script completed successfully but did not produce an openable result";
            }
        }

        return new BashRunResult(
                requestedScript,
                exitCode,
                timedOut,
                durationMs,
                stdout.text(),
                stderr.text(),
                target.orElse(null),
                openAttempted,
                openStarted,
                openError);
    }

    private Path ensureRoot() throws IOException {
        String configured = properties.getScriptsRoot();
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("scripts-root must not be blank");
        }
        Path root = Path.of(configured).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root.toRealPath();
    }

    private CapturedOutput capture(InputStream input) throws IOException {
        int maxBytes = Math.max(1, properties.getMaxOutputBytes());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(maxBytes, 16_384));
        byte[] chunk = new byte[8192];
        boolean truncated = false;
        int read;
        while ((read = input.read(chunk)) != -1) {
            int remaining = maxBytes - buffer.size();
            if (remaining > 0) {
                int accepted = Math.min(remaining, read);
                buffer.write(chunk, 0, accepted);
                if (accepted < read) {
                    truncated = true;
                }
            } else {
                truncated = true;
            }
        }
        String text = buffer.toString(StandardCharsets.UTF_8);
        if (truncated) {
            text += "\n[output truncated at " + maxBytes + " bytes]";
        }
        return new CapturedOutput(text);
    }

    private CapturedOutput awaitOutput(Future<CapturedOutput> future, String streamName) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CapturedOutput("[" + streamName + " capture interrupted]");
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
            return new CapturedOutput("[" + streamName + " capture failed: " + e.getMessage() + "]");
        }
    }

    private void terminateProcessTree(Process process) {
        try {
            List<ProcessHandle> descendants = process.toHandle().descendants().toList();
            for (ProcessHandle handle : descendants) {
                handle.destroy();
            }
            process.destroy();
            if (process.isAlive()) {
                for (ProcessHandle handle : descendants) {
                    if (handle.isAlive()) {
                        handle.destroyForcibly();
                    }
                }
                process.destroyForcibly();
            }
        } catch (Exception ignored) {
            process.destroyForcibly();
        }
    }

    private long sizeQuietly(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    private record CapturedOutput(String text) {}
}
