# Firefly — Agent Guide

## Project Overview

Firefly is a plugin-powered Spring Boot platform with **4 integrated plugins** + **embedded MCP server**:

**Core (always-on):**
- **Web Dashboard** — Thymeleaf HTML with plugin browser, system status, quick links
- **Web Terminal** — xterm.js + pty4j WebSocket bridge for running the TUI
- **REST API** — RESTful service with OpenAPI/Swagger documentation
- **MCP Server** — Model Context Protocol embedded server for AI agent integration

**Optional Plugins (drop-in runtime JAR loading):**
1. **CLI Plugin** — Terminal UI (TamboUI) with 3-panel explorer, menus, search, output history
2. **Actuator Plugin** — Spring Boot Actuator endpoints (health, metrics, info)
3. **ADO Plugin** — Azure DevOps work item management and querying
4. **MCP Registry Plugin** — MCP tool registry and UI for managing AI protocol services

All plugins auto-load from `plugins/` directory with zero server restart.

The project supports both JVM execution and GraalVM native-image compilation.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| TUI | tamboui + jline3 + picocli (cli-plugin) |
| Native | GraalVM CE 25.0.2 + native-maven-plugin |
| Terminal | xterm.js + WebSocket + pty4j |

---

## Build Commands

**Docker Compose is the supported way to build, run, and test this project.** Use it rather than a bare local `mvnw`/`java` — the host toolchain isn't guaranteed to match the Java 25 / GraalVM 25.0.2 versions this project requires; Compose pins those via `maven:3-eclipse-temurin-25` (build) and `eclipse-temurin:25-jre` (runtime).

### Docker Compose — build / run / test
```bash
# Build core app (runs unit tests as part of `mvn install`)
docker compose --profile build run --rm app-build

# Build all plugins
docker compose --profile build run --rm plugin-build

# Run a single test
docker compose --profile build run --rm app-build ./mvnw test -Dtest=FireflyApplicationTests#contextLoads

# Run the app (builds from `.docker/java/Dockerfile`, exposes port 17922)
docker compose up app --build
```

See [Local Pipeline Stages](#local-pipeline-stages-docker-compose) below for the full profile table.

### Maven (JVM) — local alternative, requires matching JDK 25 on the host
```bash
./mvnw clean install
java -jar target/firefly-*.jar
```

### Maven (Native Image) — local alternative
Requires GraalVM CE 25.0.2 (`asdf install` picks this up automatically via `.tool-versions`; make sure `JAVA_HOME` points at the GraalVM install, not just `PATH` — Maven reads `JAVA_HOME` first):
```bash
JAVA_HOME=$(asdf where java) ./mvnw clean -Pnative native:compile -DskipTests
./target/firefly
```

### Docker — native image build (any platform running Docker)
No local GraalVM install needed; builds inside `ghcr.io/graalvm/native-image-community:25` (toolchain packages installed at container start):
```bash
docker compose --profile native run --rm native-build
./target/firefly
```
The resulting binary targets whatever platform Docker itself is running on (matches `docker compose up app`'s host).

**Docker notes:**
- Base image: `eclipse-temurin:25-jre`
- Build image: `maven:3-eclipse-temurin-25`
- Port: `17922` (mapped `17922:17922` in `compose.yaml`)
- `SERVER_PORT=17922` is passed as an environment variable
- **Plugins**: `plugins/` directory is mounted to `/app/plugins` and loaded via `PropertiesLauncher` at runtime

#### Local Pipeline Stages (Docker Compose)
The `compose.yaml` mirrors the GitHub Actions pipeline stages so they can be run locally using Docker Compose profiles:

| Stage | CI Equivalent | Local Command |
|-------|--------------|---------------|
| Build App | `build-app.yml` | `docker compose --profile build run --rm app-build` |
| Build Plugins | `build-plugins.yml` | `docker compose --profile build run --rm plugin-build` |
| Run App | `run-app.yml` | `docker compose --profile verify up app verify` |
| Run App with Plugin | `run-app-with-plugin.yml` | `docker compose --profile plugin up app verify-plugin` |
| Showcase All Plugins | — | `docker compose --profile showcase up app-all-plugins showcase --build` |
| Native Image Build | — | `docker compose --profile native run --rm native-build` |
| Release (Maven + Docker) | `release.yml` | triggered by pushing a release tag — see [Releases](#releases-maven-release-plugin) below |

**Notes:**
- Use `--abort-on-container-exit` with `up` to auto-stop services after verification completes.
- The `app` service builds the JAR inside Docker; `app-build` builds against the host-mounted source.
- The `app-all-plugins` service bakes all plugin JARs into the image (no host volume mount).
- The `showcase` service verifies the dashboard, Swagger, Actuator, ADO, CLI, and MCP Registry plugins collectively.
- Maven dependencies are cached in a `maven-cache` volume for faster rebuilds.
- `build-plugins.yml` and `plugin-build` build and test all 4 plugins (`cli`, `actuator`, `ado`, `mcp-registry`) on every push/PR; `plugin-build` also flattens the built JARs into `plugins/*.jar` so the `app-all-plugins`/`showcase` Docker build can bake them in (see `.dockerignore` — plugin source subdirectories are excluded from the Docker build context, only the flattened `plugins/*.jar` files and `plugins/README.md` are copied).

#### Executable jar
The root pom's `exec-maven-plugin` execution prepends a `#!/bin/sh` launcher to the repackaged jar after `spring-boot:repackage` runs, so `target/firefly-<version>.jar` is directly runnable (`./target/firefly-<version>.jar`) instead of requiring `java -jar`. Spring Boot 4 dropped its own built-in "fully executable jar" feature (the old `<executable>true</executable>` config is a no-op now), so this replicates it manually. Same caveat the original feature had: relies on zip readers that seek the end-of-central-directory record from EOF (true for `java`, `unzip`, `jar`) rather than requiring the archive to start at offset 0 — works on Linux/macOS, not guaranteed for every zip tool.

#### Releases (maven-release-plugin)
Each of the 5 Maven projects in this repo (root `firefly`, `ado-plugin`, `actuator-plugin`, `cli-plugin`, `mcp-registry-plugin`) is independently releasable — they're separate artifacts, not reactor modules, so version/tag per project:

```bash
# Root app (tag: v<version>)
JAVA_HOME=$(asdf where java) ./mvnw release:prepare release:perform
git push origin main --follow-tags   # pushChanges=false by default — push is a separate, explicit step

# A plugin (tag: <plugin-name>-<version>), e.g. ado-plugin
cd plugins/ado-plugin
mvn release:prepare release:perform
cd ../.. && git push origin main --follow-tags
```

`release:prepare` bumps the version, commits, and tags locally only (`pushChanges=false`) — nothing reaches the remote until you `git push --follow-tags` yourself. Pushing a tag matching `v*`/`ado-plugin-*`/`actuator-plugin-*`/`cli-plugin-*`/`mcp-registry-plugin-*` triggers `.github/workflows/release.yml`, which deploys that artifact to GitHub Packages (`https://maven.pkg.github.com/munafsheikh/firefly`) using the workflow's built-in `GITHUB_TOKEN` — no manual credential setup needed for CI.

Pushing a `v*` tag (root app release) additionally builds and pushes two Docker images to GHCR (`ghcr.io/munafsheikh/firefly` and `ghcr.io/munafsheikh/firefly-all-plugins`, each tagged `<version>` and `latest`), via the `release-docker-app` and `release-docker-all-plugins` jobs — no separate tag needed for these, and no extra secrets: both jobs authenticate with the workflow's built-in `GITHUB_TOKEN` against GHCR.
- `release-docker-app`: plain core-app image, no plugins baked in (mirrors `docker compose up app`).
- `release-docker-all-plugins`: builds all 4 plugins fresh from source and bakes their JARs into the image (mirrors the `showcase` compose profile) — note this bundles whatever plugin *source* is on `main` at release time, not necessarily the last-published plugin JAR versions from GitHub Packages.

For a **local** `mvn deploy` (outside CI), add a `<server>` entry to `~/.m2/settings.xml` with a [GitHub PAT](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry) that has `write:packages` scope:
```xml
<server>
  <id>github</id>
  <username>YOUR_GITHUB_USERNAME</username>
  <password>YOUR_GITHUB_PAT</password>
</server>
```

---

## Objectives & Roadmap (PlantUML Gantt)

```plantuml
@startgantt
!theme plain

project starts 2026-04-30
saturday are closed
sunday are closed

[Web API Foundation] as [F1] starts 2026-04-30 and lasts 1 days
[Web API Foundation] -> [Actuator & Docs]

[Actuator & Docs] as [F2] starts after [F1] and lasts 1 days
[Actuator & Docs] -> [TUI Core]

[TUI Core] as [F3] starts after [F2] and lasts 2 days
[TUI Core] -> [Maven Shade Fix]

[Maven Shade Fix] as [F4] starts after [F3] and lasts 1 days
[Maven Shade Fix] -> [GraalVM Native Image]

[GraalVM Native Image] as [F5] starts after [F4] and lasts 2 days
[GraalVM Native Image] -> [Web Terminal Integration]

[Web Terminal Integration] as [F6] starts after [F5] and lasts 3 days
[Web Terminal Integration] -> [Jediterm Evaluation]

[Jediterm Evaluation] as [F7] starts after [F6] and lasts 1 days

-- Feature Breakdown --

[Web API Foundation] requires {
  [Spring Boot starter]
  [Port configuration 17922]
}

[TUI Core] requires {
  [tamboui dependencies]
  [FireflyCommand CLI]
  [Frame class fix]
}

[Maven Shade Fix] requires {
  [Execution ID isolation]
  [CLI classifier jar]
}

[GraalVM Native Image] requires {
  [GraalVM 25 install]
  [JAVA_HOME switch]
  [native-maven-plugin profile]
  [AOT processing]
}

[Web Terminal Integration] requires {
  [spring-boot-starter-websocket]
  [pty4j dependency]
  [Auto-configuration]
  [WebSocket PTY handler]
  [Terminal service]
  [xterm.js frontend]
}

[Jediterm Evaluation] requires {
  [Jediterm repo analysis]
  [Swing vs web feasibility]
  [xterm.js alternative decision]
}

-- Milestones --

[F1] happens at [F1].end
[F3] happens at [F3].end
[F5] happens at [F5].end
[F6] happens at [F6].end

@endgantt
```

---

## Upcoming Features

Planned plugins and core extensions, not yet implemented. Each follows the existing plugin conventions (own Maven module under `plugins/`, `META-INF/plugin.properties`, `firefly.plugin.<name>.*` properties prefix) unless noted otherwise.

### 1. MCP Plugin Tree — pluggable skills & MCP server definitions

Extend `mcp-registry-plugin` so third-party plugins can contribute to the MCP tree instead of only exposing the 4 built-in embedded tools.

- **MCP plugin manifest**: any plugin JAR may ship a manifest (alongside `plugin.properties`) declaring one or more **skill definitions** and/or **MCP server definitions** it provides. A plugin can declare either or both.
- **Grouping**: the registry groups all declared skills/servers by owning plugin name. In the MCP tree (both the `/pages/mcp-registry` UI and the `GET /api/mcp/registry` payload), each plugin name becomes a parent node, with its skills and servers listed as child nodes underneath — mirroring how the dashboard already groups actuator endpoints per plugin.
- **Settings per node**: selecting a skill or server child node shows a "Settings" panel rendering that entry's owning plugin's configuration (the resolved `firefly.plugin.<name>.*` properties), so a user can see which plugin a given skill/server came from and how it's configured without leaving the tree view.
- **API additions**: `GET /api/mcp/tree` (grouped, hierarchical view) and `GET /api/mcp/plugins/{name}/config` (resolved config for a given plugin's node) alongside the existing flat `/api/mcp/tools` and `/api/mcp/registry` endpoints.

### 2. Theme Manager + Theme Plugins (Obsidian-style)

A core `ThemeManager` plus a new plugin *shape* — theme plugins — that let the dashboard/terminal appearance be swapped without a rebuild, the same way Obsidian.md loads community themes.

- **Core**: `ThemeManager` service + `ThemeController` track the active theme (persisted across restarts), expose `GET/POST /api/theme` (list installed themes, get/set active theme), and inject the active theme's CSS variables into `dashboard.html`/`terminal.html` at render time.
- **Theme plugin shape**: a lightweight plugin containing only a `plugin.properties` (id/name/version/author/preview) and a bundled CSS resource (custom properties overriding the base palette — background, accent, font, terminal colors). No Spring auto-config or Java code required for a pure-CSS theme; a `ThemeProvider` SPI interface is available for themes that need to contribute template fragments too.
- **Discovery**: `ThemeManager` scans `plugins/*.jar` for theme manifests the same way `DashboardService.readPluginMetadata` scans for plugin metadata today.
- **Dashboard UI**: a "Themes" section listing installed themes with a live preview swatch and an "Activate" button; switching theme applies immediately across dashboard, terminal, and MCP registry pages via the shared CSS-variable layer.

### 3. PlantUML Plugin

A new `plantuml-plugin` that bundles the PlantUML jar and exposes text-to-diagram rendering as a service.

- **Dependency**: bundles `net.sourceforge.plantuml:plantuml` so no external Graphviz/PlantUML install is required on the host.
- **REST endpoint**: `POST /api/plantuml/render?format=svg|png` — body is raw PlantUML source, response is the rendered image (`image/svg+xml` or `image/png`).
- **Web UI**: `/pages/plantuml` — a textarea for PlantUML source with a live-updating preview pane (re-renders on debounce), useful for authoring diagrams (e.g. the Gantt roadmap above) without leaving the app.
- **Properties prefix**: `firefly.plugin.plantuml.*` (e.g. render timeout, max diagram size).

### 4. Markdown Editor Plugin

A new `markdown-plugin` providing fully functional Markdown file editing against a configured directory.

- **REST endpoints**: list/browse files under a configured root (`firefly.plugin.markdown.root-dir`), `GET`/`PUT` file content, create/rename/delete.
- **Web UI**: `/pages/markdown` — split-pane editor (raw Markdown left, rendered preview right, live-updating), file tree sidebar for navigation within the configured root.
- **Rendering**: server-side Markdown-to-HTML (e.g. flexmark/commonmark) for the preview pane, keeping parity between what's edited and what's saved.
- **Safety**: all file operations are resolved against and constrained to the configured root directory to prevent path traversal outside it.

### 5. TUI Web Browser Plugin — image/CSS/JS-capable terminal browser

A new plugin (working name `webtui-plugin`) that reimagines the 80s-style terminal browser (lynx/w3m) for the web terminal, but without their limitation of text-only rendering — since the "terminal" here is already xterm.js in a real browser tab, it can render actual images instead of ANSI-art approximations.

- **Approach**: a headless Chromium instance (Playwright, already available in this environment) navigates to the requested URL with full JS/CSS execution. Rather than degrading to text/ANSI blocks (the Browsh/lynx approach for real ttys), frames are captured as images and displayed inline in the xterm.js pane using its image-addon, giving genuine image/CSS/JS-rendered pages inside the terminal UI.
- **Input**: keyboard and mouse events captured by the PTY/WebSocket bridge are forwarded to the headless page (scroll, click, type, navigate back/forward), so it behaves like driving a real browser, just embedded in the terminal.
- **Integration point**: launched the same way `cli-plugin` is auto-detected today — `TerminalService` detects a `webtui-plugin*.jar` and offers it as a launchable target from the terminal explorer panel, alongside the existing TUI.
- **Properties prefix**: `firefly.plugin.webtui.*` (e.g. default homepage, frame rate/refresh interval, viewport size).

---

## Terminal UI (TUI) — 3-Panel Web Interface

**Access:** `http://localhost:17922/terminal`

The TUI runs in your browser via xterm.js connected to the server-side `cli-plugin` (TamboUI) over WebSocket.

### Layout
- **Left Panel** — Explorer with 4 brownable items:
  - `firefly-core` — Core Spring Boot application
  - `actuator-plugin` — Health, metrics, info endpoints
  - `ado-plugin` — Azure DevOps integration
  - `cli-plugin` — This TUI (TamboUI)
- **Center Panel** — Main content view
- **Right Panel** — Status indicators
- **Output Panel (bottom)** — Command history (up to 200 lines)

### Controls
| Key | Action |
|-----|--------|
| `Ctrl+/` | Toggle search mode |
| `↑/↓` | Navigate explorer/menus |
| `Enter` | Select item / activate menu |
| `Esc` | Close menu / exit search |
| Mouse | Full mouse support (click, drag, scroll) |

### Features
- **Menu System** — File, Edit, View menus
- **Toolbar** — RUN, BUILD, TEST, SEARCH action buttons
- **Search Filter** — Type to filter explorer items in real-time
- **Dark Theme** — Configurable appearance toggle
- **Responsive** — Adapts to terminal window size

---

## Coding Conventions

- **Package**: `ai.firefly.*` for all application code
- **Terminal component**: `ai.firefly.terminal.*` — isolated, auto-detected
- **Config**: Use `@ConfigurationProperties` with `firefly.terminal.*` prefix
- **Auto-config**: Use `@AutoConfiguration` + `@ConditionalOnClass` for drop-in behavior
- **Lombok**: Used sparingly; explicit constructors for auto-config beans

---

## Test Doc Screenshots

`ScreenshotDocumentationExtension` (`src/test/java/ai/firefly/testdoc/`) is a JUnit 5 extension that is auto-detected for the whole test run (`src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` + `junit.jupiter.extensions.autodetection.enabled=true` in `junit-platform.properties`) — it requires no per-test wiring, so it covers every existing and future test automatically.

- **What gets screenshotted**: only tests with a visually detectable outcome, i.e. ones that boot a real embedded web server (`@SpringBootTest(webEnvironment = RANDOM_PORT)`/`DEFINED_PORT`, detected via the `local.server.port` Spring property). Plain unit tests, `MOCK`/`NONE` web-environment tests, and anything with no Spring context are silently skipped — there is nothing to render.
- **Default page**: `/`. Override per test class/method with `@DocScreenshot(paths = {...})` (`src/test/java/ai/firefly/testdoc/DocScreenshot.java`) to capture additional/different pages (e.g. `/swagger-ui/index.html`, `/terminal`).
- **When**: after every applicable test, pass or fail — output file names are suffixed `PASS`/`FAIL`.
- **Output**: `target/test-screenshots/<TestClass>/<testMethod>__<PASS|FAIL>__<path>.png` — a build artifact (`target/` is gitignored), not committed.
- **Failure handling**: any Playwright problem (Chromium not installed, no network for the first download, etc.) is caught and logged as a warning; it never fails a test. The extension disables itself for the rest of the run after the first such failure rather than spamming logs.
- **Browser provisioning**: `Playwright.create()` lazily installs browsers on first use if its cache (`~/.cache/ms-playwright`) is empty — and it installs Chromium, Firefox, *and* WebKit even though this extension only launches Chromium, so the first run needs network access to all three CDNs. Pre-install just Chromium once to skip that: `./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install chromium"`, then set `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` so later runs never attempt the Firefox/WebKit downloads. Without either step the extension still degrades safely (see failure handling above) — it just retries the full three-browser install attempt every run until one succeeds. See `DashboardScreenshotTests` (`src/test/java/ai/firefly/dashboard/`) for a working example against the dashboard and Swagger UI.

---

## Directory Structure

```
src/main/java/ai/firefly/
├── FireflyApplication.java
├── dashboard/                        # Root dashboard (Thymeleaf)
│   ├── DashboardController.java
│   ├── DashboardService.java
│   └── PluginInfo.java
└── terminal/                         # Web terminal component (drop-in)
    ├── TerminalAutoConfiguration.java
    ├── TerminalController.java
    ├── TerminalProperties.java
    ├── TerminalService.java
    └── TerminalWebSocketHandler.java

src/main/resources/
├── META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
├── static/
│   └── terminal.html                 # xterm.js frontend
├── templates/
│   └── dashboard.html                # Thymeleaf dashboard
└── application.yaml
```

---

## Plugin System

Firefly supports runtime plugin loading via Spring Boot's `PropertiesLauncher`. Drop plugin JARs into `plugins/` and they are automatically added to the classpath.

### CLI Plugin (`plugins/cli-plugin/`)

- **Build**: `cd plugins/cli-plugin && mvn clean package`
- **Install**: `cp target/cli-plugin-1.0.0.jar ../../plugins/`
- **Nature**: Standalone executable fat JAR (not a Spring Boot auto-config plugin)
- **Launch**: `java -jar plugins/cli-plugin-1.0.0.jar`
- **Web terminal integration**: The terminal service auto-detects `cli-plugin*.jar` in the plugins directory and launches it automatically

### Actuator Plugin (`plugins/actuator-plugin/`)

- **Build**: `cd plugins/actuator-plugin && mvn clean package`
- **Install**: `cp target/actuator-plugin-1.0.0.jar ../../plugins/`
- **Properties prefix**: `firefly.plugin.actuator.*`
- **Auto-config**: `ActuatorPluginAutoConfiguration` registers via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **Endpoints**: Standard Spring Boot Actuator endpoints at `/actuator/*`

### Azure DevOps Plugin (`plugins/ado-plugin/`)

- **Build**: `cd plugins/ado-plugin && mvn clean package`
- **Install**: `cp target/ado-plugin-1.0.0.jar ../../plugins/`
- **Config**: `plugins/ado-plugin.yaml` or env vars (`ADO_ORG`, `ADO_PROJECT`, `ADO_PAT`)
- **Properties prefix**: `firefly.plugin.ado.*`
- **Auto-config**: `AdoPluginAutoConfiguration` registers beans via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **REST endpoints**: `/api/ado/*` — see README.md for full endpoint list

### MCP Registry Plugin (`plugins/mcp-registry-plugin/`)

**Model Context Protocol (MCP)** server for AI agent integration.

- **Build**: `cd plugins/mcp-registry-plugin && mvn clean package`
- **Install**: `cp target/mcp-registry-plugin-1.0.0.jar ../../plugins/`
- **Properties prefix**: `firefly.plugin.mcp-registry.*`
- **Storage**: JSON file-based registry (persisted to disk)
- **Auto-config**: `McpRegistryAutoConfiguration` via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **REST endpoints**:
  - `GET /api/mcp/tools` — List registered MCP tools
  - `GET /api/mcp/registry` — Get registry state
  - `GET /pages/mcp-registry` — Web UI for MCP registry management
- **Embedded MCP Tools**:
  - `ListPlugins(namespace)` — Discover installed plugins
  - `DashboardHealth()` — Real-time system status
  - `ActuatorData(endpoint)` — Fetch metrics/health/info
  - `ListActuatorEndpoints()` — Browse available endpoints
- **Web Dashboard Integration**: MCP tools are listed on the dashboard homepage under "Model Context Protocol (MCP)" section

---

## Notes for Agents

1. **Java version**: Currently 25 (downgraded from 26 for GraalVM compatibility)
2. **Native image**: Built with `graalvm-community-25.0.2`
3. **Port**: Default server port is 17922
4. **Terminal**: The web terminal is auto-detected — it only activates when `pty4j` and Spring WebSocket are on the classpath
5. **TUI classpath**: The web terminal auto-detects `cli-plugin*.jar` in the plugins directory and launches it directly
6. **Plugin architecture**: Plugins are standalone Maven projects. Spring-based plugins use `provided` Spring Boot deps and auto-configuration imports. The CLI plugin is a standalone fat JAR.
7. **Dashboard**: The root URL (`/`) renders a Thymeleaf dashboard showing installed plugins, actuator endpoints (if plugin loaded), and quick links
