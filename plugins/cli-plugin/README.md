# Firefly CLI Plugin

A standalone TamboUI + jline3 + picocli terminal application (`FireflyCommand`) providing a 3-panel TUI (Explorer / Preview / Status) with a menu bar, toolbar, and search filter. This is **not** a Spring-managed plugin — it's a self-contained fat JAR launched as a child process.

## Build

```bash
cd plugins/cli-plugin
mvn clean package
```

Produces `target/cli-plugin-<version>-jar-with-dependencies.jar` via `maven-assembly-plugin`.

## Install

```bash
cp target/cli-plugin-*-jar-with-dependencies.jar ../../plugins/cli-plugin-1.0.0.jar
```

## How it's launched

- **Standalone**: `java -jar plugins/cli-plugin-1.0.0.jar`
- **Web terminal integration**: `ai.firefly.terminal.TerminalService.findCliPluginJar()` auto-detects `cli-plugin*.jar` in the plugins directory and launches it as the PTY's child process, so it's what renders at `/terminal` in the browser via xterm.js.

## UI

- **Left panel** — Explorer listing `firefly-core`, `actuator-plugin`, `ado-plugin`, `cli-plugin`
- **Center panel** — Preview of the selected item + an Output log (last 200 lines)
- **Right panel** — Status (Java version, OS, user, PID)
- **Menu bar** — File / Edit / View / Terminal / Help, opened via `Alt+<letter>` or mouse click
- **Toolbar** — Run / Build / Test / Search buttons
- Full mouse support (click, drag, scroll) alongside keyboard navigation (`Tab`, arrows, `Ctrl+/` search, `Esc`)

## Testing

```bash
cd plugins/cli-plugin
mvn test
```

Almost all of `FireflyCommand` is rendering code (`render*` methods) driven by a real `Frame`/`TuiRunner` from a live terminal session, which isn't something a headless unit test can exercise. What *is* unit-tested (via reflection, since the class exposes no other seam) is the pure state-transition logic: panel focus cycling, theme toggling, search filtering, menu/toolbar bookkeeping, and output-log capping. This is why the plugin's jacoco/pitest thresholds (`jacoco.line.ratio` / `pitest.mutation.threshold` in `pom.xml`) are set lower than the other plugins — rendering correctness is verified manually by running the app and using `/terminal` (see the "Test Doc Screenshots" section in `AGENTS.md` and the `run` skill), not by mutation-tested unit coverage.
