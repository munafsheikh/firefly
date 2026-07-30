# Web Dashboard

The root page (`/`), rendered server-side with Thymeleaf (`DashboardController` + `DashboardService`, template `templates/dashboard.html`).

![Dashboard](screenshots/dashboard.png)

## What it shows

- **What Can You Do?** — quick links to Swagger, the web terminal, Actuator, and this Documentation Browser
- **Installed Plugins** — one row per JAR in `plugins/` that has a `META-INF/plugin.properties` (id, name, version, description); JARs without one still show up using the filename as a fallback
- **Actuator Endpoints** — only rendered if `actuator-plugin` is loaded; fetched live via `RestClient` against `/actuator`
- **🎨 Themes** — every installed theme (the built-in `default` plus any theme plugin), with an Activate button; see [Theme Manager](/docs/theme-manager)
- **Alert Balloons** — a demo of the shared `fireflyShowAlert(type, message)` JS helper (used by `fragments/alert.html`) for toast-style notifications

## How plugin discovery works

`DashboardService.getInstalledPlugins()` lists `*.jar` under the configured plugins directory (`firefly.plugins.path`, default `plugins`) and opens each one just far enough to read `META-INF/plugin.properties` — it never needs those plugins on the classpath. The same scan pattern (directory of JARs → open just the one metadata entry you need) is reused by `ThemeManager`, `McpPluginTreeScanner`, and `DocsManager` for their own conventions.

## Theming

Every color in `dashboard.html` is a CSS custom property (`--firefly-bg`, `--firefly-accent`, etc.) with today's look as the default. A `<link rel="stylesheet" href="/api/theme/active.css">` loads *after* the page's own `<style>` block, so an active theme plugin's overrides win by cascade order alone — no template changes needed to reskin the page.
