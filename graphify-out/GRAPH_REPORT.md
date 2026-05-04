# Graph Report - /home/m/code/github/munafsheikh/firefly  (2026-05-04)

## Corpus Check
- 16 files · ~17,411 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 173 nodes · 195 edges · 27 communities detected
- Extraction: 83% EXTRACTED · 17% INFERRED · 1% AMBIGUOUS · INFERRED: 33 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]

## God Nodes (most connected - your core abstractions)
1. `AdoWorkItemService` - 16 edges
2. `AdoPluginController` - 14 edges
3. `AdoRestClient` - 11 edges
4. `AdoPluginMetadata` - 8 edges
5. `AdoPluginAutoConfiguration` - 7 edges
6. `PtySession` - 7 edges
7. `TerminalAutoConfiguration` - 6 edges
8. `FireflyCommand` - 5 edges
9. `TerminalWebSocketHandler` - 5 edges
10. `TUI/CLI` - 5 edges

## Surprising Connections (you probably didn't know these)
- `Firefly Project` --semantically_similar_to--> `Firefly Project`  [INFERRED] [semantically similar]
  CLAUDE.md → AGENTS.md
- `Firefly Project` --semantically_similar_to--> `Firefly`  [INFERRED] [semantically similar]
  CLAUDE.md → README.md
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

### Community 0 - "Community 0"
Cohesion: 0.09
Nodes (2): AdoPluginController, AdoWorkItemService

### Community 1 - "Community 1"
Cohesion: 0.16
Nodes (3): PtySession, TerminalService, TerminalWebSocketHandler

### Community 2 - "Community 2"
Cohesion: 0.35
Nodes (1): AdoRestClient

### Community 3 - "Community 3"
Cohesion: 0.24
Nodes (11): Firefly Project, FireflyCommand CLI, jline3, picocli, TamboUI, TUI/CLI, Command Pattern, CommandRegistry (+3 more)

### Community 4 - "Community 4"
Cohesion: 0.24
Nodes (1): AdoPluginMetadata

### Community 5 - "Community 5"
Cohesion: 0.2
Nodes (10): Web API, AI Integration, AiService, Azure DevOps Integration, AzureDevOpsClient, Prompts Directory, Redis, RedisTemplate (+2 more)

### Community 6 - "Community 6"
Cohesion: 0.22
Nodes (9): GraalVM CE 25.0.2, GraalVM Native Image, Java 25, native-maven-plugin, Rationale: Java 25 Downgrade for GraalVM, GraalVM Native Image, Java 26, native-maven-plugin (+1 more)

### Community 7 - "Community 7"
Cohesion: 0.25
Nodes (1): AdoPluginAutoConfiguration

### Community 8 - "Community 8"
Cohesion: 0.38
Nodes (1): TerminalAutoConfiguration

### Community 9 - "Community 9"
Cohesion: 0.33
Nodes (1): FireflyCommand

### Community 10 - "Community 10"
Cohesion: 0.33
Nodes (6): compose.yaml, Container-First Development, Rationale: Container-First Development, TestcontainersConfiguration, Docker Compose Support, Testcontainers Support

### Community 11 - "Community 11"
Cohesion: 0.4
Nodes (5): TerminalAutoConfiguration, TerminalController, TerminalProperties, TerminalService, TerminalWebSocketHandler

### Community 12 - "Community 12"
Cohesion: 0.5
Nodes (1): TerminalController

### Community 13 - "Community 13"
Cohesion: 0.5
Nodes (4): Liquibase, Oracle DB, Rationale: Never Use ddl-auto=update, Spring Data JPA

### Community 14 - "Community 14"
Cohesion: 0.5
Nodes (4): pty4j, Web Terminal Integration, WebSocket, xterm.js

### Community 15 - "Community 15"
Cohesion: 0.67
Nodes (1): TestFireflyApplication

### Community 16 - "Community 16"
Cohesion: 0.67
Nodes (1): FireflyApplicationTests

### Community 17 - "Community 17"
Cohesion: 0.67
Nodes (1): FireflyApplication

### Community 18 - "Community 18"
Cohesion: 0.67
Nodes (3): RabbitConfig, RabbitMQ, RabbitTemplate

### Community 19 - "Community 19"
Cohesion: 0.67
Nodes (3): Rationale: Favor Integration Tests, Rationale: No Mockito for Infrastructure, Testing Approach

### Community 20 - "Community 20"
Cohesion: 1.0
Nodes (1): AdoPluginProperties

### Community 21 - "Community 21"
Cohesion: 1.0
Nodes (1): TestcontainersConfiguration

### Community 22 - "Community 22"
Cohesion: 1.0
Nodes (1): TerminalProperties

### Community 23 - "Community 23"
Cohesion: 1.0
Nodes (2): SSH Integration, SshService

### Community 24 - "Community 24"
Cohesion: 1.0
Nodes (2): Thymeleaf Templates, Thymeleaf

### Community 25 - "Community 25"
Cohesion: 1.0
Nodes (2): Spring Boot 4.0.6, Spring Boot 4.0.6

### Community 26 - "Community 26"
Cohesion: 1.0
Nodes (1): Jediterm Evaluation

## Ambiguous Edges - Review These
- `Java 26` → `Java 25`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **39 isolated node(s):** `AdoPluginProperties`, `TestcontainersConfiguration`, `TerminalProperties`, `CommandRegistry`, `WorkItem` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 20`** (2 nodes): `AdoPluginProperties`, `AdoPluginProperties.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 21`** (2 nodes): `TestcontainersConfiguration.java`, `TestcontainersConfiguration`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 22`** (2 nodes): `TerminalProperties.java`, `TerminalProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 23`** (2 nodes): `SSH Integration`, `SshService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 24`** (2 nodes): `Thymeleaf Templates`, `Thymeleaf`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 25`** (2 nodes): `Spring Boot 4.0.6`, `Spring Boot 4.0.6`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 26`** (1 nodes): `Jediterm Evaluation`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Java 26` and `Java 25`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `AdoPluginMetadata` connect `Community 4` to `Community 1`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `AdoWorkItemService` connect `Community 0` to `Community 4`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **What connects `AdoPluginProperties`, `TestcontainersConfiguration`, `TerminalProperties` to the rest of the system?**
  _39 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._