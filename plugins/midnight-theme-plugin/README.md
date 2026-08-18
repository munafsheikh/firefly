# Firefly Midnight Theme Plugin

A minimal example theme plugin proving the theme-plugin shape — no Java code at all.

## Build

```bash
cd plugins/midnight-theme-plugin
mvn clean package
```

## Install

```bash
cp target/midnight-theme-plugin-*.jar ../../plugins/
```

## Contents

- `META-INF/plugin.properties` — id `midnight-theme`, read by `DashboardService.readPluginMetadata` to list it on the dashboard
- `META-INF/firefly/theme.css` — overrides the dashboard's CSS custom-property palette (`--firefly-bg`, `--firefly-surface`, `--firefly-text`, etc.) to a dark scheme

## How theming works

Core-owned `ThemeManager` (`ai.firefly.theme`, not a plugin) scans `plugins/*.jar` for a bundled `META-INF/firefly/theme.css` alongside `META-INF/plugin.properties`. Once installed, activate it via the dashboard's "🎨 Themes" card or `POST /api/theme/midnight-theme`. A theme needs zero Java code — pure CSS + `plugin.properties` metadata, so it only needs to override the custom properties it cares about; unset ones fall back to the default palette by cascade order.

## Testing and coverage exemption

This plugin ships no Java source (`src/main/java` is empty), so it's exempt from the jacoco/pitest/test-class requirements in `scripts/check-plugin-completeness.sh` — there's nothing to unit test. Verify a theme change by running the app and activating it from the dashboard.
