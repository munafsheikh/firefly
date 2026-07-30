# MCP Registry Plugin

Two related jobs: a CRUD registry for **external** MCP servers Firefly should know about, and the scanner/UI for the **plugin-contributed** skill/server tree (see [Model Context Protocol](/docs/mcp-server) for the concept).

![MCP Registry page — external servers and the plugin tree](screenshots/mcp-registry-page.png)

## Install

```bash
cd plugins/mcp-registry-plugin && mvn clean package
cp target/mcp-registry-plugin-1.0.1-SNAPSHOT.jar ../../plugins/
```

## Configuration

Properties prefix: `firefly.plugin.mcp-registry.*` — `enabled` (default `true`), `store-path` (default `~/.firefly/registry.json`, JSON file-based persistence for registered external servers).

## External MCP server registry

| Endpoint | Description |
|---|---|
| `GET /api/mcp-registry/mcps` | List registered external MCP servers |
| `POST /api/mcp-registry/mcps` | Register one |
| `GET /api/mcp-registry/mcps/{id}` | Fetch one |
| `DELETE /api/mcp-registry/mcps/{id}` | Remove one |
| `POST /api/mcp-registry/mcps/{id}/refresh` | Refresh its cached state |

## Plugin skill/server tree

| Endpoint | Description |
|---|---|
| `GET /api/mcp-registry/tree` | Every plugin bundling a `META-INF/firefly/mcp-plugin.json`, grouped by plugin |
| `GET /api/mcp-registry/tree/{pluginId}/config` | That plugin's resolved config (secret-looking keys masked) |
| `GET /mcp-registry` | Web UI — both the external-server table and the expandable plugin tree with a per-node Settings panel |

`McpPluginTreeScanner` finds the manifest the same way `DashboardService` finds `plugin.properties` — by opening each JAR in the plugins directory just far enough to check for the one entry it cares about.
