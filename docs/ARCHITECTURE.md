# Firefly — System Architecture

> **Diagram type**: C4 container/component
> **Scope**: Firefly Spring Boot application + plugins
> **Assumption**: Single JVM process, embedded MCP server on stdio
> **Runtime**: Java 25 (GraalVM CE 25.0.2 for native image)
> **Port**: 17922

---

## 1. Container / Component View

```mermaid
C4Context
  Person(browser, "Browser User", "Developer or operator accessing the web UI")
  Person(ai_agent, "AI Agent", "Claude / Copilot / Codex CLI via MCP protocol")

  System_Boundary(firefly, "Firefly Application") {
    Container(web, "Spring Boot Web", "Java 25, Spring Boot 4.0.6", "Serves Thymeleaf pages, REST API, Swagger UI on port 17922")
    Container(ws, "WebSocket Terminal", "Spring WebSocket + pty4j", "Bridges xterm.js ↔ PTY child process at /ws/terminal")
    Container(mcp, "MCP Server", "Embedded stdio JSON-RPC 2.0", "AI agent protocol — tool discovery and invocation")
    ContainerDb(plugin_registry, "Plugin Registry", "JAR files in plugins/", "Drop-in runtime classpath via PropertiesLauncher")

    Container_Boundary(plugins, "Plugins (drop-in JARs)") {
      Component(cli_plugin, "CLI Plugin", "tamboui + jline3 + picocli", "3-panel TUI for browsing/menus/search")
      Component(actuator_plugin, "Actuator Plugin", "Spring Boot Actuator", "Health, metrics, info at /actuator/*")
      Component(ado_plugin, "ADO Plugin", "Azure DevOps REST client", "Work item management and queries")
      Component(mcp_registry, "MCP Registry Plugin", "Spring Boot plugin", "MCP tool registry and web UI")
    }
  }

  Rel(browser, web, "HTTP GET /, /swagger-ui.html, /pages/*", "HTTPS")
  Rel(browser, ws, "WebSocket /ws/terminal", "xterm.js + PTY bridge")
  Rel(ai_agent, mcp, "JSON-RPC 2.0 over stdio", "tools/list, tools/call")
  Rel(web, plugin_registry, "Scans plugins/ for JARs", "PropertiesLauncher")
  Rel(ws, cli_plugin, "Starts as child process", "java -jar cli-plugin.jar")
  Rel(actuator_plugin, web, "Exposes /actuator/* endpoints", "HTTP")
  Rel(ado_plugin, web, "Exposes /api/ado/* endpoints", "HTTP")
```

---

## 2. Terminal Session — Runtime Sequence

```mermaid
sequenceDiagram
  participant Browser as Browser (xterm.js)
  participant WS as WebSocket Handler
  participant Service as TerminalService
  participant PTY as PTY Child Process

  Browser->>WS: WebSocket connect (ws://host:17922/ws/terminal)
  activate WS

  WS->>Service: createSession(outputConsumer)
  activate Service
  Service->>Service: buildCommand() — find cli-plugin*.jar
  Service->>PTY: PtyProcessBuilder.start()
  activate PTY
  PTY-->>Service: PtyProcess (PID)
  Service->>Service: startReaders() — virtual thread reading stdout
  Service-->>WS: PtySession
  deactivate Service

  WS-->>Browser: TextMessage("") — connection accepted
  deactivate WS

  Note over Browser,PTY: ---- Session Active ----

  Browser->>Browser: term.onData("ls\n")
  Browser->>WS: TextMessage("ls\n")
  WS->>PTY: write("ls\n")
  PTY-->>PTY: Process.exec ls
  PTY-->>Service: stdout chunk ("total 42\n...")
  Service-->>WS: outputConsumer.accept(...)
  WS-->>Browser: TextMessage("total 42\n...")
  Browser->>Browser: term.write(event.data)

  Note over Browser,PTY: ---- Session Active ----

  Browser->>Browser: window.resize event
  Browser->>WS: TextMessage("\x01{cols},{rows}")
  WS->>PTY: setWinSize(cols, rows)
  PTY-->>PTY: SIGWINCH received

  Note over Browser,PTY: ---- Session Terminated ----

  Browser->>WS: WebSocket close / disconnect
  WS->>PTY: destroy()
  PTY-->>WS: process exited
  WS-->>Browser: (connection closed)
  deactivate PTY
```

### Terminal WebSocket Protocol

| Direction | Prefix | Payload | Purpose |
|-----------|--------|---------|---------|
| Browser → Server | (none) | Raw text | Terminal input (keystrokes, commands) |
| Browser → Server | `\x01` | `{cols},{rows}` | Terminal resize (e.g. `\x01120,40`) |
| Browser → Server | `\x02` | `{json}` | Control message — alert, status (extensible) |
| Server → Browser | (none) | Raw bytes | PTY output (ANSI + text) |
| Server → Browser | `\x02` | `{json}` | Control message — alert, lifecycle |

**Control message JSON schema:**
```json
{
  "type": "alert",
  "level": "info" | "success" | "warning" | "error",
  "message": "Human-readable message"
}
```

### Invariants

- A WebSocket session owns at most one PTY process.
- Closing the session must terminate the owned process.
- Output may only be sent to the owning browser session.
- Plugin processes may only launch from approved JAR locations.

### Unspecified

- Authentication
- Process sandboxing
- Resource limits
- Reconnection
- Backpressure

---

## 3. Deployment / Trust Boundary

```mermaid
C4Deployment
  Person(user, "User", "Developer or operator")

  System_Boundary(host, "Host Machine") {
    ContainerDb(filesystem, "File System", "plugins/ directory, application.yaml")

    System_Boundary(docker, "Docker Container (eclipse-temurin:25-jre)") {
      Container(jvm, "JVM", "Java 25 / GraalVM CE 25.0.2")
      Container(app, "Firefly App", "Spring Boot 4.0.6 JAR on port 17922")

      Boundary(trust, "Trust Boundary") {
        Component(http_server, "Web Server", "Tomcat embedded — HTTP + WebSocket")
        Component(mcp_stdio, "MCP Server (stdio)", "stdin/stdout — no network exposure")
      }

      Boundary(child, "Child Process Boundary") {
        Component(cli_process, "CLI Plugin PTY", "tamboui TUI — ANSI terminal")
      }
    }
  }

  Rel(user, http_server, "HTTPS :17922", "Browser")
  Rel(user, mcp_stdio, "stdio", "AI agent CLI")
  Rel(app, filesystem, "Reads", "Scans plugins/ directory")
  Rel(app, cli_process, "Spawns", "pty4j fork")
  Rel(cli_process, filesystem, "Reads (java -jar)", "JAR file access")
```

---

## 4. Terminal Session — State Model

```mermaid
stateDiagram-v2
  [*] --> CONNECTING: WebSocket open

  CONNECTING --> STARTING_PROCESS: WS session created
  CONNECTING --> TRANSPORT_ERROR: WS handshake failed

  STARTING_PROCESS --> ACTIVE: PTY started, readers online
  STARTING_PROCESS --> START_FAILED: Plugin JAR not found
  STARTING_PROCESS --> START_FAILED: PtyProcessBuilder error

  ACTIVE --> PROCESS_EXITED: Child process terminated (exit 0)
  ACTIVE --> PROCESS_EXITED: Child process crashed (non-zero)
  ACTIVE --> TRANSPORT_ERROR: WebSocket broken / timeout
  ACTIVE --> CLOSING: User disconnect (browser close / refresh)

  PROCESS_EXITED --> CLOSING
  START_FAILED --> CLOSING
  TRANSPORT_ERROR --> CLOSING

  CLOSING --> CLOSED: PTY destroyed, reader thread interrupted
  CLOSED --> [*]

  note right of ACTIVE
    Normal operation:
    - PTY stdout → WebSocket → xterm.js
    - term.onData → WebSocket → PTY stdin
    - Resize → SIGWINCH
    - Control messages (alert, status)
  end note

  note right of START_FAILED
    Typical causes:
    - No cli-plugin*.jar in plugins/
    - Java version mismatch
    - Insufficient memory
  end note
```

---

## 5. Plugin Loading Architecture

```mermaid
flowchart LR
  subgraph Startup
    A[PropertiesLauncher] --> B["Scan plugins/*.jar"]
    B --> C{Has spring.factories?}
    C -->|Yes| D[Auto-configure beans]
    C -->|No| E[Load as standalone JAR]
  end

  subgraph Runtime
    F[DashboardService] --> G["Scan plugins/*.jar at /api/dashboard"]
    G --> H[Return PluginInfo list]
  end

  subgraph Terminal
    I[TerminalService.buildCommand] --> J["Scan plugins/cli-plugin*.jar"]
    J --> K[Spawn PTY with java -jar]
  end
```

---

## 6. Alert System Architecture

```mermaid
flowchart LR
  subgraph Server
    C[Controller] -->|model.addAttribute alerts| T[Thymeleaf]
    W[WebSocket Handler] -->|\\x02 JSON control msg| F[Frontend JS]
  end

  subgraph Client
    T -->|th:replace fragments/alert| H[alert.html fragment]
    F -->|fireflyShowAlert()| H
    H -->|auto-dismiss 5s| U[User sees balloon]
  end

  subgraph Alert Types
    S[success - green]
    I[info - blue]
    W2[warning - yellow]
    E[error - red]
  end
```

---

## Directory Structure

```
src/main/java/ai/firefly/
├── FireflyApplication.java
├── dashboard/           # Web dashboard — Thymeleaf + REST
│   ├── DashboardController.java
│   ├── DashboardService.java
│   ├── PluginInfo.java
│   ├── AlertMessage.java         ← NEW
│   ├── OverviewPageController.java
│   └── CliPageController.java
├── terminal/            # Web terminal — WebSocket + pty4j
│   ├── TerminalAutoConfiguration.java
│   ├── TerminalController.java
│   ├── TerminalProperties.java
│   ├── TerminalService.java
│   └── TerminalWebSocketHandler.java

src/main/resources/
├── templates/
│   ├── dashboard.html
│   ├── overview.html
│   ├── cli-page.html
│   └── fragments/
│       └── alert.html            ← NEW (reusable alert fragment)
├── static/
│   └── terminal.html
└── application.yaml

docs/
├── ARCHITECTURE.md               ← NEW (this file)
└── MCP-ARCHITECTURE.md           (existing)
```

---

## Related Documents

- `README.md` — Build/run/test instructions
- `AGENTS.md` — Agent guidance, coding conventions, roadmap
- `docs/MCP-ARCHITECTURE.md` — MCP server design, implementation plan, phases 1–3
- `compose.yaml` — Docker Compose profiles for all pipeline stages
