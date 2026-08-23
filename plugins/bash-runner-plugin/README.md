# Firefly Bash Runner Plugin

Runs Bash scripts from a configured scripts directory, waits for completion, and opens a successful result with `xdg-open`.

## Build and install

```bash
cd plugins/bash-runner-plugin
mvn clean package
cp target/bash-runner-plugin-*.jar ../../plugins/
```

Restart Firefly after installing a newly built JAR. The plugin auto-configures through Spring Boot's `AutoConfiguration.imports` mechanism.

## Script contract

Scripts live below `~/.firefly/scripts` by default. Firefly invokes them as an argv list using `ProcessBuilder`; request arguments are never interpolated into a shell command.

A script should print the result it wants opened as:

```bash
#!/usr/bin/env bash
set -euo pipefail

out="${TMPDIR:-/tmp}/firefly-report.html"
printf '<h1>Hello from Firefly</h1>\n' > "$out"
printf 'FIREFLY_OPEN=%s\n' "$out"
```

If no `FIREFLY_OPEN=` marker is printed, the plugin also accepts the last non-blank stdout line when it is an existing file/directory or an `http://`/`https://` URL.

`xdg-open` is launched only after the script exits successfully (`0`). Firefly does not wait for the opened desktop application to close.

## Browser UI

Open:

```text
http://localhost:17922/bash-runner
```

The UI lists discovered `.sh` files, accepts one argument per line, waits for the script, and displays stdout/stderr, duration, exit code, timeout state, and the opened result.

## REST API

```bash
curl -s http://localhost:17922/api/bash-runner/scripts

curl -s \
  -H 'Content-Type: application/json' \
  -d '{"script":"render-report.sh","args":["demo"],"open":true}' \
  http://localhost:17922/api/bash-runner/run
```

Health:

```bash
curl -s http://localhost:17922/api/bash-runner/health
```

## Configuration

```properties
firefly.plugin.bash-runner.enabled=true
firefly.plugin.bash-runner.scripts-root=${user.home}/.firefly/scripts
firefly.plugin.bash-runner.bash-command=bash
firefly.plugin.bash-runner.timeout=5m
firefly.plugin.bash-runner.max-output-bytes=1048576
firefly.plugin.bash-runner.xdg-open-enabled=true
firefly.plugin.bash-runner.xdg-open-command=xdg-open
```

## Security model

This plugin intentionally executes local code, so treat access to its endpoint as privileged. It does **not** accept arbitrary inline shell commands. Only `.sh` files below the configured scripts root can run; normalized path traversal and symlink escapes outside that root are rejected. Arguments are passed directly to `ProcessBuilder`, output capture is bounded, and timed-out process trees are terminated.

`xdg-open` runs on the same host/session as the Firefly JVM. If Firefly is inside Docker or on a remote SSH server without a desktop session, opening a GUI on your workstation requires the appropriate display/session or a host-side bridge; the plugin cannot make a container's `xdg-open` magically execute on the client machine.
