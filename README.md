# Firefly

A Java TUI with TamboUI, Thymeleaf + Spring Boot, RabbitMQ, Redis, Oracle DB, Bash, AI and Azure DevOps integration.

## Quick Start

### Prerequisites

- Docker & Docker Compose — the supported way to build, run, and test this project
- (Optional, local-only alternative) Java 25 + Maven 3.9+ / `./mvnw`, GraalVM CE 25.0.2 for native builds — only if your host JDK matches the project's toolchain

## Building, Running, and Testing

### Option 1: Docker Compose (recommended)

```bash
# Build the core app (runs unit tests as part of `mvn install`)
docker compose --profile build run --rm app-build

# Build all plugins
docker compose --profile build run --rm plugin-build

# Run the app
docker compose up app --build
```

The application is exposed on **port 17922**. See `AGENTS.md` for the full table of Compose profiles (build/verify/plugin/showcase) and how they map to CI.

**Plugins:** Drop built plugin JARs into the `plugins/` directory before starting. These are mounted read-only into the container and loaded at runtime via Spring Boot's `PropertiesLauncher`.

### Option 2: Maven (JVM) — local alternative

```bash
./mvnw clean package
./target/firefly-*.jar     # directly executable, or: java -jar target/firefly-*.jar
```

The application will start on **port 17922**.

### Option 3: Maven (Native Image) — local alternative

Requires GraalVM CE 25.0.2 (`JAVA_HOME` must point at it — `asdf` users: `JAVA_HOME=$(asdf where java)`):

```bash
JAVA_HOME=$(asdf where java) ./mvnw -Pnative native:compile -DskipTests
./target/firefly
```

Or build inside Docker (no local GraalVM needed; targets whatever platform Docker runs on):

```bash
docker compose --profile native run --rm native-build
./target/firefly
```

Native image starts in ~61ms.

### Azure DevOps Plugin

The ADO plugin connects Firefly to Azure DevOps for work item management.

**Build (Docker):**
```bash
docker compose --profile build run --rm plugin-build
```

**Build (local Maven alternative):**
```bash
cd plugins/ado-plugin
mvn clean package
```

**Install:**
```bash
cp plugins/ado-plugin/target/ado-plugin-1.0.0.jar plugins/
```

**Configure:** Copy `plugins/ado-plugin.yaml` to `plugins/ado-plugin.yaml` and set your values, or use environment variables:
```yaml
firefly:
  plugin:
    ado:
      enabled: true
      url: https://dev.azure.com/my-org
      project: my-project
      pat: ${ADO_PAT}
```

**Endpoints exposed when plugin is active:**
| Endpoint | Description |
|----------|-------------|
| `GET /api/ado/health` | Plugin connection status |
| `GET /api/ado/workitems/{id}` | Get a work item |
| `POST /api/ado/workitems/query` | Query work items (WIQL) |
| `GET /api/ado/workitems/open-bugs` | Get all open bugs |
| `GET /api/ado/workitems/tasks?user={name}` | Get tasks for user |
| `PATCH /api/ado/workitems/{id}/state` | Update work item state |
| `PATCH /api/ado/workitems/{id}/assign` | Assign work item |
| `POST /api/ado/tasks` | Create a task |
| `POST /api/ado/bugs` | Create a bug |
| `GET /api/ado/projects` | List projects |

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `http://localhost:17922` | Main web application |
| `http://localhost:17922/terminal.html` | Web terminal (xterm.js) |
| `ws://localhost:17922/ws/terminal` | WebSocket for terminal PTY |

## Project Structure

```
src/main/java/ai/firefly/
├── FireflyApplication.java
├── cli/
│   └── FireflyCommand.java          # TUI entry point
└── terminal/                         # Web terminal component (auto-detected)
    ├── TerminalAutoConfiguration.java
    ├── TerminalController.java
    ├── TerminalProperties.java
    ├── TerminalService.java
    └── TerminalWebSocketHandler.java
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| TUI | tamboui + jline3 + picocli |
| Native | GraalVM CE 25.0.2 |
| Terminal | xterm.js + WebSocket + pty4j |

## Configuration

Key settings in `src/main/resources/application.yaml`:

```yaml
server:
  port: 17922
```

## Development

See `AGENTS.md` for detailed agent-oriented documentation, feature roadmap, and coding conventions.

## Releases

Each Maven project here (root `firefly`, plus `ado-plugin`/`actuator-plugin`/`cli-plugin`) is independently versioned and released via `maven-release-plugin`. See [AGENTS.md § Releases](AGENTS.md#releases-maven-release-plugin) for the `release:prepare`/`release:perform` workflow and GitHub Packages deploy setup.
