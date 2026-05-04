package ai.firefly.terminal;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class TerminalService {

    private final TerminalProperties properties;

    public TerminalService(TerminalProperties properties) {
        this.properties = properties;
    }

    public PtySession createSession(Consumer<String> outputConsumer) throws IOException {
        String[] command = buildCommand();

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");

        PtyProcessBuilder builder = new PtyProcessBuilder(command)
                .setEnvironment(env)
                .setConsole(false)
                .setRedirectErrorStream(true);

        PtyProcess process = builder.start();

        PtySession session = new PtySession(process, outputConsumer);
        session.startReaders();

        log.info("Started terminal session with PID {}", process.pid());
        return session;
    }

    private String[] buildCommand() {
        String[] base = new String[]{properties.getCommand()};
        String args = properties.getCommandArgs();
        if (args == null || args.isBlank()) {
            return base;
        }
        String[] split = args.split("\\s+");
        String[] result = new String[base.length + split.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(split, 0, result, base.length, split.length);
        return result;
    }

    public static class PtySession {

        private final PtyProcess process;
        private final Consumer<String> outputConsumer;
        private Thread readerThread;

        public PtySession(PtyProcess process, Consumer<String> outputConsumer) {
            this.process = process;
            this.outputConsumer = outputConsumer;
        }

        void startReaders() {
            readerThread = Thread.ofVirtual().start(() -> {
                try (InputStreamReader isr = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(isr)) {
                    char[] buffer = new char[8192];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        outputConsumer.accept(new String(buffer, 0, read));
                    }
                } catch (IOException e) {
                    if (!process.isAlive()) {
                        return;
                    }
                    log.warn("Terminal read error", e);
                }
            });
        }

        public void write(String data) throws IOException {
            if (process.isAlive()) {
                process.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().flush();
            }
        }

        public void resize(int cols, int rows) {
            if (process.isAlive()) {
                process.setWinSize(new com.pty4j.WinSize(cols, rows));
            }
        }

        public void close() {
            if (readerThread != null) {
                readerThread.interrupt();
            }
            process.destroy();
        }

        public boolean isAlive() {
            return process.isAlive();
        }
    }
}
