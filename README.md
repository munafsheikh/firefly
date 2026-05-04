# Firefly

A Java TUI with TamboUI, Thymeleaf + Spring Boot, RabbitMQ, Redis, Oracle DB, Bash, AI and Azure DevOps integration.

## Quick Start

### Prerequisites

- Java 25 (GraalVM CE 25.0.2 recommended for native image builds)
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker & Docker Compose (optional, for containerized runtime)

## Running the Application

### Option 1: Maven (JVM)

```bash
./mvnw clean package
java -jar target/firefly-*.jar
```

The application will start on **port 17922**.

### Option 2: Maven (Native Image)

Requires GraalVM CE 25.0.2:

```bash
./mvnw -Pnative native:compile -DskipTests
./target/firefly
```

Native image starts in ~61ms.

### Option 3: Docker Compose

```bash
docker compose up --build
```

The application is exposed on **port 17922**.

**Plugins:** Drop built plugin JARs into the `plugins/` directory before starting. These are mounted read-only into the container and loaded at runtime via Spring Boot's `PropertiesLauncher`.

### Azure DevOps Plugin

The ADO plugin connects Firefly to Azure DevOps for work item management.

**Build:**
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
