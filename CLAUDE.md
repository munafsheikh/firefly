# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Firefly — Java TUI app integrating TamboUI, Thymeleaf, Spring Boot, RabbitMQ, Redis, Oracle DB, AI, and Azure DevOps. Currently in early scaffolding phase.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run single test
./mvnw test -Dtest=FireflyApplicationTests#contextLoads

# Run with Testcontainers dev mode
./mvnw spring-boot:test-run

# Native image (GraalVM)
./mvnw native:compile -Pnative
```

## Stack

- **Java 26**, Spring Boot 4.0.6, Spring MVC (`spring-boot-starter-webmvc`)
- **REST client**: `spring-boot-starter-restclient` (not WebClient/RestTemplate)
- **API docs**: SpringDoc OpenAPI 3 at `/swagger-ui.html`
- **Actuator**: enabled via `spring-boot-starter-actuator`
- **Lombok**: annotation processing wired in both compile and test-compile phases
- **Docker Compose**: `spring-boot-docker-compose` auto-manages `compose.yaml` on startup
- **Testcontainers**: JUnit 5 integration; `TestcontainersConfiguration` is the shared `@TestConfiguration` — add containers there
- **GraalVM native**: `native-maven-plugin` present; keep reflection/proxy usage GraalVM-compatible

## Architecture notes

Base package: `ai.firefly`. Entry point: `FireflyApplication`.

`compose.yaml` is currently empty — add services (RabbitMQ, Redis, Oracle) there; Spring Boot will start/stop them automatically in dev.

`TestFireflyApplication` bootstraps the app with `TestcontainersConfiguration` for local dev against live containers without a full Docker Compose stack.

## CLI / Command pattern

Firefly is a TUI application. Model all user-facing operations as commands:
- Each command implements a shared `Command` interface (`execute`, `undo` if reversible, `help`)
- Commands are registered in a central `CommandRegistry` and dispatched by name/alias
- TUI input loop reads a line, tokenises, looks up command, delegates execution
- Side-effectful commands (SSH, Azure DevOps, AI) are async by default — wrap in `CompletableFuture`, surface progress to TUI via a callback/event bus

## TamboUI

TamboUI drives all terminal rendering. Rules:
- Never write directly to `System.out` from business logic — route through TamboUI components
- Layouts are declared programmatically; keep layout construction in dedicated `*View` classes separate from data-fetching logic
- Refresh only dirty regions; avoid full-screen redraws on incremental data updates

## Azure DevOps integration

Used for ticket retrieval. Encapsulate behind an `AzureDevOpsClient` service using `RestClient` (already on classpath).
- Credentials sourced from `application.yaml` (`azure.devops.org`, `azure.devops.pat`) — never hardcoded
- Ticket/work-item responses mapped to internal `WorkItem` record; no Azure SDK types leak outside the client package
- Cache work-item lists in Redis with a short TTL; bust on explicit refresh command

## AI integration

Wrap AI calls (Claude / Azure OpenAI) behind an `AiService` interface.
- Use `RestClient` — no Langchain4j or Spring AI dependency unless explicitly added
- Stream responses to TUI incrementally; do not buffer full completion before display
- Prompts live in `src/main/resources/prompts/` as `.txt` or `.md` files, loaded via `ClassPathResource`
- Keep model name and endpoint in `application.yaml`; swap models without code changes

## SSH

Use Apache MINA SSHD or JSch (add as dependency when implementing).
- `SshService` manages a pool of `ClientSession` objects keyed by host
- Execute remote commands via `ChannelExec`; stream stdout/stderr back to TUI
- SSH config (hosts, keys) in `application.yaml` under `ssh.hosts[*]`

## RabbitMQ

Declare exchanges, queues, and bindings as `@Bean`s in a `RabbitConfig` class.
- Use `RabbitTemplate` for outbound; `@RabbitListener` for inbound
- Message payloads serialised as JSON via `Jackson2JsonMessageConverter` — register it in `RabbitConfig`
- Add `rabbitmq` service to `compose.yaml` and mirror it with a `RabbitMQContainer` in `TestcontainersConfiguration`

## Redis

Used for caching (Azure DevOps work items, AI responses) and session state.
- Inject `RedisTemplate<String, Object>` with `GenericJackson2JsonRedisSerializer` for value serialisation
- Cache abstractions via Spring `@Cacheable` where TTL is acceptable; use `RedisTemplate` directly for finer control
- Add `redis` service to `compose.yaml` and mirror with `GenericContainer("redis:8-alpine")` in `TestcontainersConfiguration`

## Oracle DB

Accessed via Spring Data JPA.
- Use `@Entity` + repository pattern; keep entities in `ai.firefly.domain`
- Liquibase (or Flyway) for schema migrations — add to `pom.xml` when implementing; migrations in `src/main/resources/db/changelog/`
- Add Oracle container (`gvenzl/oracle-free`) to `TestcontainersConfiguration` for integration tests
- Never use `spring.jpa.hibernate.ddl-auto=update` in any environment

## Thymeleaf

Used for HTML rendering (web views alongside the TUI).
- Templates in `src/main/resources/templates/`; fragments in `templates/fragments/`
- Controllers annotated `@Controller` (not `@RestController`) return view names; never return raw HTML strings
- Model attributes populated via `Model` parameter — no `ModelAndView`

## Testing

**Favour integration tests over unit tests.** Prefer `@SpringBootTest` with real containers over mocks.
- All infrastructure containers (RabbitMQ, Redis, Oracle) declared in `TestcontainersConfiguration` — tests import it via `@Import(TestcontainersConfiguration.class)`
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` for full-stack HTTP tests
- Use `MockMvcTester` (Spring Boot 4 `spring-boot-starter-webmvc-test`) for controller slice tests when a full context is wasteful
- Test coverage target: all public service methods and all REST/TUI command handlers
- No Mockito mocks for infrastructure — if it calls a database, queue, or cache, test against the real container

## Container-first development

- All services run in containers; local JVM connects to them via `compose.yaml` (dev) or Testcontainers (test)
- `spring-boot-docker-compose` starts `compose.yaml` automatically on `./mvnw spring-boot:run` — no manual `docker compose up` needed
- Add new infrastructure services to **both** `compose.yaml` (dev) and `TestcontainersConfiguration` (test) simultaneously
- Production target is a GraalVM native image — keep all code native-compatible (no runtime reflection without `@RegisterReflectionForBinding`, no dynamic proxies outside Spring's own infrastructure)

## Project structure

```
.docker/
  java/Dockerfile        # App image — multi-stage: Maven build → GraalVM native or JRE runtime
  redis/Dockerfile       # Redis with any custom config layered on top of redis:8-alpine
  rabbitmq/Dockerfile    # RabbitMQ with management plugin + custom definitions baked in
compose.yaml             # Dev orchestration — references .docker/*/Dockerfile via build.context
src/
  main/
    java/ai/firefly/     # All application code (base package)
    resources/
      application.yaml   # All config; no .properties files
      prompts/           # AI prompt templates (*.md or *.txt)
      templates/         # Thymeleaf HTML templates
        fragments/       # Reusable Thymeleaf fragments
      db/changelog/      # Liquibase migrations
  test/
    java/ai/firefly/
      TestcontainersConfiguration.java  # Shared container beans for all tests
      TestFireflyApplication.java       # Dev entrypoint with containers
```

## Containerisation dependencies

`compose.yaml` service entries must reference the local Dockerfiles:

```yaml
services:
  app:
    build: { context: ., dockerfile: .docker/java/Dockerfile }
  redis:
    build: { context: .docker/redis }
  rabbitmq:
    build: { context: .docker/rabbitmq }
```

`TestcontainersConfiguration` mirrors each service:
- `rabbitmq` → `RabbitMQContainer("rabbitmq:4-management-alpine")`
- `redis` → `GenericContainer("redis:8-alpine")`
- `oracle` → `OracleContainer("gvenzl/oracle-free:latest-faststart")`

`.docker/java/Dockerfile` multi-stage pattern:
1. Stage `build` — `maven:3-eclipse-temurin-26` → `./mvnw package -DskipTests` (tests run in CI, not image build)
2. Stage `native` — `ghcr.io/graalvm/native-image:26` → compile to binary
3. Stage `runtime` — `gcr.io/distroless/static` → copy binary, minimal attack surface
