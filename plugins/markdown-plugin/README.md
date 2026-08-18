# Firefly Markdown Plugin

A full read/write/preview Markdown file editor, scoped to a configured root directory.

## Build

```bash
cd plugins/markdown-plugin
mvn clean package
```

## Install

```bash
cp target/markdown-plugin-*.jar ../../plugins/
```

## Configuration

Properties prefix: `firefly.plugin.markdown.*`

| Property | Default | Description |
|---|---|---|
| `firefly.plugin.markdown.enabled` | `true` | Enable/disable the plugin |
| `firefly.plugin.markdown.rootDir` | `~/.firefly/markdown` | Root directory files are scoped to (auto-created) |

## Path safety

`MarkdownFileService` resolves every relative path against the root, normalizes it, requires the result to stay under the root, and additionally resolves the nearest existing ancestor's *real* path to reject symlink escapes. Any traversal attempt (`../`, absolute paths) is rejected before touching the filesystem.

## REST endpoints

- `GET /api/markdown/health`
- `GET /api/markdown/files` — list files
- `GET /api/markdown/files/**` / `PUT /api/markdown/files/**` / `DELETE /api/markdown/files/**` — read/write/delete one file
- `POST /api/markdown/preview` — raw markdown body → rendered HTML (flexmark, CommonMark + GFM tables)

## Web UI

`/pages/markdown` — file-tree sidebar, raw-markdown textarea, debounced live HTML preview, Save/New/Delete.

## Auto-config

`MarkdownPluginAutoConfiguration` registers via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, gated by `@ConditionalOnProperty(prefix = "firefly.plugin.markdown", name = "enabled", matchIfMissing = true)`.

## Testing

```bash
cd plugins/markdown-plugin
mvn test
```
