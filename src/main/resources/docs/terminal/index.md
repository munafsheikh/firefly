# Web Terminal

`http://localhost:17922/terminal` — an xterm.js terminal in the browser, bridged over a WebSocket to a real PTY process on the server.

![Terminal](screenshots/terminal.png)

## How it's wired

- `ai.firefly.terminal.TerminalAutoConfiguration` is itself conditional: `@ConditionalOnClass(PtyProcess.class)` + `@ConditionalOnProperty(firefly.terminal.enabled)`, so the whole feature simply doesn't activate if `pty4j` (or Spring WebSocket) isn't on the classpath. It's part of the core app, not a plugin, but uses the same auto-configuration mechanism plugins do.
- `TerminalWebSocketHandler` bridges the WebSocket connection to a `TerminalService.PtySession`, which spawns the PTY child process and streams its stdout back over a virtual thread.
- `TerminalService.findCliPluginJar()` scans the plugins directory for a `cli-plugin*.jar`. If found, that jar is launched directly as the PTY's child process — that's how a plain terminal turns into the full TamboUI experience below. If no CLI plugin is installed, the PTY just runs a bare shell.

## The TUI (when `cli-plugin` is installed)

A 3-panel terminal UI built with **TamboUI** + **picocli**, running as a standalone process (not a Spring bean — `cli-plugin` is a self-contained fat JAR, launched by the terminal service the same way a user would run `java -jar cli-plugin.jar`):

- **Left** — Explorer (installed components: `firefly-core`, `actuator-plugin`, `ado-plugin`, `cli-plugin`)
- **Center** — main content view
- **Right** — status indicators
- **Bottom** — output/command history (up to 200 lines)

Controls: `Ctrl+/` toggles search, arrow keys navigate, `Enter` selects, `Esc` closes, full mouse support (click/drag/scroll).
