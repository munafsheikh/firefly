# Firefly — Agent Guide

## Project Overview

Firefly is a Spring Boot application with a plugin-based architecture:
1. **Web API** — RESTful service with Thymeleaf dashboard and OpenAPI docs
2. **TUI/CLI** — Terminal UI application built on `tamboui` + `picocli` (via `cli-plugin`)
3. **Actuator** — Spring Boot Actuator endpoints (via `actuator-plugin`)

Core features (dashboard, terminal, swagger) are always available. Actuator and CLI are optional plugins.

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
Requires GraalVM CE 25.0.2:
```bash
./mvnw clean -Pnative native:compile -DskipTests
./target/firefly
```

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

**Notes:**
- Use `--abort-on-container-exit` with `up` to auto-stop services after verification completes.
- The `app` service builds the JAR inside Docker; `app-build` builds against the host-mounted source.
- The `app-all-plugins` service bakes all plugin JARs into the image (no host volume mount).
- The `showcase` service verifies the dashboard, Swagger, Actuator, ADO, and CLI plugins collectively.
- Maven dependencies are cached in a `maven-cache` volume for faster rebuilds.

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

## Coding Conventions

- **Package**: `ai.firefly.*` for all application code
- **Terminal component**: `ai.firefly.terminal.*` — isolated, auto-detected
- **Config**: Use `@ConfigurationProperties` with `firefly.terminal.*` prefix
- **Auto-config**: Use `@AutoConfiguration` + `@ConditionalOnClass` for drop-in behavior
- **Lombok**: Used sparingly; explicit constructors for auto-config beans

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

---

## Notes for Agents

1. **Java version**: Currently 25 (downgraded from 26 for GraalVM compatibility)
2. **Native image**: Built with `graalvm-community-25.0.2`
3. **Port**: Default server port is 17922
4. **Terminal**: The web terminal is auto-detected — it only activates when `pty4j` and Spring WebSocket are on the classpath
5. **TUI classpath**: The web terminal auto-detects `cli-plugin*.jar` in the plugins directory and launches it directly
6. **Plugin architecture**: Plugins are standalone Maven projects. Spring-based plugins use `provided` Spring Boot deps and auto-configuration imports. The CLI plugin is a standalone fat JAR.
7. **Dashboard**: The root URL (`/`) renders a Thymeleaf dashboard showing installed plugins, actuator endpoints (if plugin loaded), and quick links
