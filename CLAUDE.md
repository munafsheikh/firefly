# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Firefly — Spring Boot app, plugin-based architecture. Core always-on: web dashboard (Thymeleaf), web terminal (xterm.js + pty4j over WebSocket), Swagger/OpenAPI. Optional features ship as separate Maven modules under `plugins/`, built independently and loaded at runtime via Spring Boot's `PropertiesLauncher` (drop JAR in `plugins/`, app picks it up off classpath — no rebuild of core app needed).

See `AGENTS.md` for full roadmap, CI/Docker Compose profile mapping, and per-plugin build/install/config instructions — don't duplicate that here, read it directly when touching plugins or compose profiles.

## Commands

**Build, run, and test through Docker Compose — that's the supported workflow in this repo.** The host JVM/Maven setup is not guaranteed to match the project's Java 25 toolchain; Compose builds inside `maven:3-eclipse-temurin-25` and runs inside `eclipse-temurin:25-jre`, so don't rely on a bare local `./mvnw`/`java` unless you've separately confirmed the host has a matching JDK.

```bash
# Build the core app (mirrors CI "Build App"; runs unit tests as part of `mvn install`)
docker compose --profile build run --rm app-build

# Build all plugins (mirrors CI "Build Plugins")
docker compose --profile build run --rm plugin-build

# Run a single test inside the build container
docker compose --profile build run --rm app-build ./mvnw test -Dtest=FireflyApplicationTests#contextLoads

# Run the core app (plugins/ mounted read-only, picked up by PropertiesLauncher)
docker compose up app --build

# Run with all plugins baked into the image + smoke-test dashboard/Swagger/Actuator/ADO/CLI together
docker compose --profile showcase up app-all-plugins showcase --build

# Run + verify (mirrors CI "Run App" / "Run App with Plugin")
docker compose --profile verify up app verify
docker compose --profile plugin up app verify-plugin
```

Each plugin is its own Maven module under `plugins/<name>-plugin/`, built the same way as `plugin-build` does internally (`cd plugins/<name>-plugin && mvn clean package` inside the `maven:3-eclipse-temurin-25` container), then copied into the shared `plugins/` dir for the running app to pick up.

App listens on port **17922** (not Spring Boot's default 8080) — set in `application.yaml` and mirrored via `SERVER_PORT` in Docker Compose. Full profile-to-CI mapping is in `AGENTS.md`.

## Architecture

- **Java 25** (downgraded from 26 for GraalVM compatibility), Spring Boot 4.0.6, `spring-boot-starter-webmvc` + `spring-boot-starter-restclient` (no WebClient/RestTemplate).
- Base package `ai.firefly`; entry point `FireflyApplication`.
- **Plugins are independent Maven projects**, not modules of the root `pom.xml`. Each builds its own JAR with its own `pom.xml` under `plugins/<name>-plugin/`. They are *not* on the compile classpath of the core app — discovery happens at runtime by scanning the `plugins/` directory.
- Two plugin shapes:
  - **Spring auto-config plugins** (`actuator-plugin`, `ado-plugin`): declare Spring Boot as `provided`, register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, gated by `@ConditionalOnProperty(prefix = "firefly.plugin.<name>", name = "enabled", matchIfMissing = true)`. Each carries a `META-INF/plugin.properties` (id/name/version/description/author) that `DashboardService.readPluginMetadata` reads to list it on the dashboard.
  - **Standalone CLI plugin** (`cli-plugin`): a self-contained fat JAR (TamboUI + jline3 + picocli), not Spring-managed. `TerminalService.findCliPluginJar()` auto-detects `cli-plugin*.jar` in the plugins directory and launches it as the PTY's child process — this is how the web terminal at `/terminal` gets a TUI to run.
- **Terminal component** (`ai.firefly.terminal`): `TerminalAutoConfiguration` is itself conditional — `@ConditionalOnClass(PtyProcess.class)` + `@ConditionalOnProperty(firefly.terminal.enabled)` — so it only activates when `pty4j` and Spring WebSocket are present. `TerminalWebSocketHandler` bridges the WebSocket to a `TerminalService.PtySession`, which spawns the PTY process and streams stdout via a virtual thread.
- **Dashboard** (`ai.firefly.dashboard`): root `/` renders `templates/dashboard.html`, listing installed plugins (scanned from `plugins/*.jar` metadata) and, if the actuator plugin is loaded, fetches `/actuator` endpoints via `RestClient` to surface them.
- `compose.yaml` defines profile-gated services (`build`, `verify`/`test`, `plugin`, `showcase`) that mirror the GitHub Actions workflows in `.github/workflows/`; check that file before assuming a `docker compose` invocation needs flags not already encoded in a profile.
- `TestcontainersConfiguration` (`src/test`) is currently an empty shared `@TestConfiguration` — no infra containers (DB/queue/cache) are wired up yet; add bean definitions there if/when persistence or messaging lands.
- `graphify-out/` and `app.log` are generated artifacts, not source — ignore when reasoning about architecture.
