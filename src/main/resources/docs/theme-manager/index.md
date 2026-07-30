# Theme Manager

Core-owned (`ai.firefly.theme`), not a plugin — discovers **theme plugins** the way Obsidian.md discovers community themes, and serves the active one's CSS dynamically so switching themes needs no template rebuild.

![Themes card on the dashboard](screenshots/theme-manager.png)

## Pieces

- **`ThemeManager`** — scans `plugins/*.jar` for a bundled `META-INF/firefly/theme.css` alongside `META-INF/plugin.properties` (same scan pattern as `DashboardService`'s plugin listing). The built-in `default` theme always exists and needs no plugin. The active theme id is persisted to `firefly.theme.state-path` (default `~/.firefly/theme.state`) so it survives restarts, and falls back to `default` automatically if the previously-active theme's plugin JAR is later removed.
- **`ThemeController`**:
  - `GET /api/theme` → `{"activeThemeId": "...", "themes": [...]}`
  - `POST /api/theme/{id}` → activate a theme (`400` if unknown)
  - `GET /api/theme/active.css` → the active theme's raw CSS

## The theming contract

`dashboard.html` (and this Documentation Browser) define their whole palette as CSS custom properties — `--firefly-bg`, `--firefly-surface`, `--firefly-text`, `--firefly-text-muted`, `--firefly-border`, `--firefly-accent`, `--firefly-accent-2`, `--firefly-header-text`, `--firefly-link-list-bg`, `--firefly-link-list-bg-hover` — with the current look as defaults. A `<link rel="stylesheet" href="/api/theme/active.css">` is placed *after* the page's own inline `<style>` block, so a theme's `:root` overrides win by plain CSS cascade order.

## Writing a theme plugin

A theme needs **zero Java code**: just `META-INF/plugin.properties` (id/name/version/author/description, same as any plugin) and `META-INF/firefly/theme.css` overriding whichever variables it cares about. See the [Midnight Theme Plugin](/docs/midnight-theme) for a complete, minimal example — its entire implementation is two resource files.
