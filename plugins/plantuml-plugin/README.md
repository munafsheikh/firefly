# Firefly PlantUML Plugin

Bundles the PlantUML library and renders diagram source to an image, so diagrams can be authored/previewed without an external PlantUML install.

## Build

```bash
cd plugins/plantuml-plugin
mvn clean package
```

## Install

```bash
cp target/plantuml-plugin-*.jar ../../plugins/
```

## Configuration

Properties prefix: `firefly.plugin.plantuml.*`

| Property | Default | Description |
|---|---|---|
| `firefly.plugin.plantuml.enabled` | `true` | Enable/disable the plugin |
| `firefly.plugin.plantuml.renderTimeoutSeconds` | `10` | Max time a render may take before being aborted |
| `firefly.plugin.plantuml.maxSourceLength` | `20000` | Max accepted PlantUML source length |

## REST endpoints

- `GET /api/plantuml/health`
- `POST /api/plantuml/render?format=svg|png` — body: raw PlantUML source (`text/plain`) → image bytes with matching content type, or a clean 400/JSON error for invalid/oversized input (never a stack trace). Rendering runs on a bounded worker thread so a pathological diagram can't hang the request past `renderTimeoutSeconds`.

## Web UI

`/pages/plantuml` — textarea + debounced live SVG/PNG preview.

## Auto-config

`PlantumlPluginAutoConfiguration` registers via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, gated by `@ConditionalOnProperty(prefix = "firefly.plugin.plantuml", name = "enabled", matchIfMissing = true)`.

## Testing

```bash
cd plugins/plantuml-plugin
mvn test
```
