# Firefly Actuator Plugin

Exposes standard Spring Boot Actuator endpoints (health, metrics, info) inside a Firefly app, and folds them into the app's existing Swagger/OpenAPI UI.

## Build

```bash
cd plugins/actuator-plugin
mvn clean package
```

## Install

```bash
cp target/actuator-plugin-*.jar ../../plugins/
```

Drop the JAR into the running app's `plugins/` directory — no core app rebuild needed (`PropertiesLauncher` picks it up on next start).

## Configuration

Properties prefix: `firefly.plugin.actuator.*`

| Property | Default | Description |
|---|---|---|
| `firefly.plugin.actuator.enabled` | `true` | Enable/disable the plugin |

## What it does

- `ActuatorPluginAutoConfiguration` registers via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, gated by `@ConditionalOnProperty(prefix = "firefly.plugin.actuator", name = "enabled", matchIfMissing = true)`.
- `ActuatorSwaggerEnvironmentPostProcessor` runs before springdoc evaluates its actuator conditionals, and sets `springdoc.show-actuator=true` — this can only be enabled safely once this plugin's actuator classes are actually on the classpath, so it's done here rather than in the core app's config.
- `ActuatorPageController` serves `/pages/actuator`, a dashboard page describing the plugin.
- Standard Actuator endpoints are exposed at `/actuator/*` (health, info, metrics, etc.) using Spring Boot's normal Actuator auto-configuration.

## Dashboard metadata

`META-INF/plugin.properties` (id `actuator`) is read by `DashboardService.readPluginMetadata` to list this plugin on the root dashboard.

## Testing

```bash
cd plugins/actuator-plugin
mvn test
```

Covers the environment post-processor's property precedence, the page controller, and the auto-configuration's conditional wiring (enabled/disabled).
