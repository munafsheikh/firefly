# Firefly MCP Registry Plugin

Registers external MCP (Model Context Protocol) servers, and groups plugin-contributed skills/MCP-server definitions into a browsable tree.

## Build

```bash
cd plugins/mcp-registry-plugin
mvn clean package
```

## Install

```bash
cp target/mcp-registry-plugin-*.jar ../../plugins/
```

## Configuration

Properties prefix: `firefly.plugin.mcp-registry.*`

| Property | Default | Description |
|---|---|---|
| `firefly.plugin.mcp-registry.enabled` | `true` | Enable/disable the plugin |
| `firefly.plugin.mcp-registry.store-path` | — | Path to the JSON file the registry persists to |

## External MCP server registry

`McpRegistryController`:
- `GET/POST /api/mcp-registry/mcps`
- `GET/DELETE /api/mcp-registry/mcps/{id}`
- `POST /api/mcp-registry/mcps/{id}/refresh`
- `GET /mcp-registry` — Thymeleaf UI listing registered MCP servers and the plugin tree

## MCP plugin tree

`McpTreeController`, `McpPluginTreeScanner`, `McpPluginConfigResolver`: any plugin JAR in `plugins/` may bundle a `META-INF/firefly/mcp-plugin.json` manifest declaring `skills` and/or `servers` it contributes. The scanner groups these by owning plugin (id/name/version from that plugin's `plugin.properties`) into parent nodes with skills/servers as children.

- `GET /api/mcp-registry/tree` — grouped tree: `[{pluginId, pluginName, pluginVersion, skills: [...], servers: [...]}, ...]`
- `GET /api/mcp-registry/tree/{pluginId}/config` — that plugin's resolved `firefly.plugin.<pluginId>.*` configuration, read live off the Spring `Environment`; keys containing `token`/`password`/`secret`/`pat`/`key`/`credential` are masked (`****`) before being returned

## Auto-config

`McpRegistryAutoConfiguration` registers via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Testing

```bash
cd plugins/mcp-registry-plugin
mvn test
```
