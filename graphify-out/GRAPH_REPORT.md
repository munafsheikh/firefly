# Graph Report - .  (2026-04-30)

## Corpus Check
- Corpus is ~3,153 words - fits in a single context window. You may not need a graph.

## Summary
- 115 nodes · 111 edges · 22 communities detected
- Extraction: 83% EXTRACTED · 16% INFERRED · 1% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Terminal Service Implementation|Terminal Service Implementation]]
- [[_COMMUNITY_TUI CLI Core|TUI CLI Core]]
- [[_COMMUNITY_Azure DevOps AI|Azure DevOps AI]]
- [[_COMMUNITY_GraalVM Native Image|GraalVM Native Image]]
- [[_COMMUNITY_Web Terminal Frontend|Web Terminal Frontend]]
- [[_COMMUNITY_Terminal Auto-Config Java|Terminal Auto-Config Java]]
- [[_COMMUNITY_FireflyCommand TUI|FireflyCommand TUI]]
- [[_COMMUNITY_Container-First Development|Container-First Development]]
- [[_COMMUNITY_Terminal Component Architecture|Terminal Component Architecture]]
- [[_COMMUNITY_Terminal Controller|Terminal Controller]]
- [[_COMMUNITY_Database Persistence|Database Persistence]]
- [[_COMMUNITY_Test Application|Test Application]]
- [[_COMMUNITY_Application Tests|Application Tests]]
- [[_COMMUNITY_Main Application|Main Application]]
- [[_COMMUNITY_RabbitMQ Messaging|RabbitMQ Messaging]]
- [[_COMMUNITY_Testing Strategy|Testing Strategy]]
- [[_COMMUNITY_Testcontainers Config|Testcontainers Config]]
- [[_COMMUNITY_Terminal Properties|Terminal Properties]]
- [[_COMMUNITY_SSH Integration|SSH Integration]]
- [[_COMMUNITY_Thymeleaf Templates|Thymeleaf Templates]]
- [[_COMMUNITY_Spring Boot Framework|Spring Boot Framework]]
- [[_COMMUNITY_Jediterm Evaluation|Jediterm Evaluation]]

## God Nodes (most connected - your core abstractions)
1. `PtySession` - 7 edges
2. `TerminalAutoConfiguration` - 6 edges
3. `FireflyCommand` - 5 edges
4. `TerminalWebSocketHandler` - 5 edges
5. `TUI/CLI` - 5 edges
6. `TerminalService` - 4 edges
7. `Firefly Project` - 4 edges
8. `Firefly Project` - 4 edges
9. `GraalVM Native Image` - 4 edges
10. `Web Terminal Integration` - 4 edges

## Surprising Connections (you probably didn't know these)
- `Firefly Project` --semantically_similar_to--> `Firefly Project`  [INFERRED] [semantically similar]
  CLAUDE.md → AGENTS.md
- `Firefly` --semantically_similar_to--> `Firefly Project`  [INFERRED] [semantically similar]
  README.md → CLAUDE.md
- `Command Pattern` --semantically_similar_to--> `FireflyCommand CLI`  [INFERRED] [semantically similar]
  CLAUDE.md → AGENTS.md
- `TamboUI` --semantically_similar_to--> `TamboUI`  [INFERRED] [semantically similar]
  CLAUDE.md → AGENTS.md
- `compose.yaml` --semantically_similar_to--> `Docker Compose Support`  [INFERRED] [semantically similar]
  CLAUDE.md → HELP.md

## Hyperedges (group relationships)
- **Web Terminal Component** — agents_terminal_auto_configuration, agents_terminalcontroller, agents_terminalproperties, agents_terminalservice, agents_terminalwebsockethandler, terminal_html [INFERRED 0.80]
- **TUI Stack** — claude_tamboui, agents_tamboui, agents_jline3, agents_picocli, agents_fireflycommand_cli [INFERRED 0.80]
- **Native Image Pipeline** — claude_graalvm_native, agents_graalvm_native_image, agents_graalvm_ce_25_0_2, agents_native_maven_plugin, claude_native_maven_plugin [INFERRED 0.85]

## Communities

### Community 0 - "Terminal Service Implementation"
Cohesion: 0.16
Nodes (3): PtySession, TerminalService, TerminalWebSocketHandler

### Community 1 - "TUI CLI Core"
Cohesion: 0.24
Nodes (11): Firefly Project, FireflyCommand CLI, jline3, picocli, TamboUI, TUI/CLI, Command Pattern, CommandRegistry (+3 more)

### Community 2 - "Azure DevOps AI"
Cohesion: 0.2
Nodes (10): Web API, AI Integration, AiService, Azure DevOps Integration, AzureDevOpsClient, Prompts Directory, Redis, RedisTemplate (+2 more)

### Community 3 - "GraalVM Native Image"
Cohesion: 0.22
Nodes (9): GraalVM CE 25.0.2, GraalVM Native Image, Java 25, native-maven-plugin, Rationale: Java 25 Downgrade for GraalVM, GraalVM Native Image, Java 26, native-maven-plugin (+1 more)

### Community 4 - "Web Terminal Frontend"
Cohesion: 0.33
Nodes (8): pty4j, Web Terminal Integration, WebSocket, xterm.js, FitAddon, sendResize, WebSocket Terminal Connection, xterm.js

### Community 5 - "Terminal Auto-Config Java"
Cohesion: 0.38
Nodes (1): TerminalAutoConfiguration

### Community 6 - "FireflyCommand TUI"
Cohesion: 0.33
Nodes (1): FireflyCommand

### Community 7 - "Container-First Development"
Cohesion: 0.33
Nodes (6): compose.yaml, Container-First Development, Rationale: Container-First Development, TestcontainersConfiguration, Docker Compose Support, Testcontainers Support

### Community 8 - "Terminal Component Architecture"
Cohesion: 0.4
Nodes (5): TerminalAutoConfiguration, TerminalController, TerminalProperties, TerminalService, TerminalWebSocketHandler

### Community 9 - "Terminal Controller"
Cohesion: 0.5
Nodes (1): TerminalController

### Community 10 - "Database Persistence"
Cohesion: 0.5
Nodes (4): Liquibase, Oracle DB, Rationale: Never Use ddl-auto=update, Spring Data JPA

### Community 11 - "Test Application"
Cohesion: 0.67
Nodes (1): TestFireflyApplication

### Community 12 - "Application Tests"
Cohesion: 0.67
Nodes (1): FireflyApplicationTests

### Community 13 - "Main Application"
Cohesion: 0.67
Nodes (1): FireflyApplication

### Community 14 - "RabbitMQ Messaging"
Cohesion: 0.67
Nodes (3): RabbitConfig, RabbitMQ, RabbitTemplate

### Community 15 - "Testing Strategy"
Cohesion: 0.67
Nodes (3): Rationale: Favor Integration Tests, Rationale: No Mockito for Infrastructure, Testing Approach

### Community 16 - "Testcontainers Config"
Cohesion: 1.0
Nodes (1): TestcontainersConfiguration

### Community 17 - "Terminal Properties"
Cohesion: 1.0
Nodes (1): TerminalProperties

### Community 18 - "SSH Integration"
Cohesion: 1.0
Nodes (2): SSH Integration, SshService

### Community 19 - "Thymeleaf Templates"
Cohesion: 1.0
Nodes (2): Thymeleaf Templates, Thymeleaf

### Community 20 - "Spring Boot Framework"
Cohesion: 1.0
Nodes (2): Spring Boot 4.0.6, Spring Boot 4.0.6

### Community 21 - "Jediterm Evaluation"
Cohesion: 1.0
Nodes (1): Jediterm Evaluation

## Ambiguous Edges - Review These
- `Java 26` → `Java 25`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **35 isolated node(s):** `TestcontainersConfiguration`, `TerminalProperties`, `CommandRegistry`, `WorkItem`, `AiService` (+30 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Testcontainers Config`** (2 nodes): `TestcontainersConfiguration.java`, `TestcontainersConfiguration`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Terminal Properties`** (2 nodes): `TerminalProperties.java`, `TerminalProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `SSH Integration`** (2 nodes): `SSH Integration`, `SshService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Thymeleaf Templates`** (2 nodes): `Thymeleaf Templates`, `Thymeleaf`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Spring Boot Framework`** (2 nodes): `Spring Boot 4.0.6`, `Spring Boot 4.0.6`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Jediterm Evaluation`** (1 nodes): `Jediterm Evaluation`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Java 26` and `Java 25`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Firefly Project` connect `TUI CLI Core` to `Azure DevOps AI`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Why does `Web API` connect `Azure DevOps AI` to `TUI CLI Core`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **What connects `TestcontainersConfiguration`, `TerminalProperties`, `CommandRegistry` to the rest of the system?**
  _35 weakly-connected nodes found - possible documentation gaps or missing edges._