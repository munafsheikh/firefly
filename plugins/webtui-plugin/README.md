# Firefly WebTUI Browser Plugin

A modern take on the 80s-style terminal browser (lynx/w3m): rather than degrading to text/ANSI art the way real ttys must, this drives a real headless Chromium instance (Playwright) server-side with full JS/CSS execution and serves the rendered page as an actual PNG screenshot to a normal web page — genuine image/CSS/JS-rendered browsing, not an approximation. A scoped-down single-shared-page MVP, not a full remote-desktop protocol.

## Build

```bash
cd plugins/webtui-plugin
mvn clean package
```

## Install

```bash
cp target/webtui-plugin-*.jar ../../plugins/
```

## Configuration

Properties prefix: `firefly.plugin.webtui.*`

| Property | Default | Description |
|---|---|---|
| `firefly.plugin.webtui.enabled` | `true` | Enable/disable the plugin |
| `firefly.plugin.webtui.homepage` | `https://example.com` | Initial page loaded on first navigation |
| `firefly.plugin.webtui.viewportWidth` / `viewportHeight` | `1280` / `800` | Headless browser viewport size |
| `firefly.plugin.webtui.navigationTimeoutSeconds` | `15` | Max time a navigation may take |

## Graceful degradation

`BrowserSessionService` lazily launches headless Chromium on first use; if launch fails (e.g. the slim JRE runtime image lacks Chromium's native shared libraries — no OS deps are baked into the core Docker image for this), every endpoint returns a clean `503` with `browserAvailable: false` instead of a stack trace or crash. Playwright's Java bindings aren't thread-safe, so all browser calls are pinned to one dedicated background thread.

## REST endpoints

- `GET /api/webtui/health` — `{"plugin":"webtui","status":"ok","browserAvailable":bool}`
- `POST /api/webtui/navigate` — `{"url":...}`
- `GET /api/webtui/screenshot` — `image/png`
- `POST /api/webtui/click` — `{"x":..,"y":..}`
- `POST /api/webtui/scroll` — `{"deltaY":..}`
- `POST /api/webtui/type` — `{"text":".."}`
- `POST /api/webtui/back`, `POST /api/webtui/forward`

## Web UI

`/pages/webtui` — URL bar + Go/Back/Forward, an `<img>` showing the latest screenshot (re-polled after each action), clicks on the image translated to viewport coordinates and forwarded as `/click`, wheel events forwarded as `/scroll`.

## Note

Running this plugin in the default Docker runtime image requires Chromium's OS-level shared libraries, which aren't installed there by default (keeping the core image lean) — it works out of the box wherever those are present (e.g. this repo's dev sandbox has Chromium pre-installed for Playwright).

## Auto-config

`WebtuiPluginAutoConfiguration` — additionally gated by `@ConditionalOnClass(Playwright.class)`, mirroring how `ai.firefly.terminal.TerminalAutoConfiguration` only activates when `pty4j` is on the classpath.

## Testing

```bash
cd plugins/webtui-plugin
mvn test
```
