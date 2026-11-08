#!/usr/bin/env bash
# Docker-first runner for a built Firefly jar (used by CI smoke tests and locally).
#
#   scripts/run-app.sh start [--plugins]   start target/firefly-*.jar in the pinned JRE image
#   scripts/run-app.sh logs                print the container log
#   scripts/run-app.sh stop                stop and remove the container
#
# Uses host networking so the app is reachable on localhost:17922, runs as the invoking
# uid:gid, and mounts the repository plus ~/.firefly (scripts for the bash-runner plugin).
# Environment: FIREFLY_RUN_IMAGE, FIREFLY_CONTAINER (default firefly-ci), FIREFLY_DOCKER_ARGS.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${FIREFLY_RUN_IMAGE:-eclipse-temurin:25-jre@sha256:ecdc0adb37bde1a281c4d245a3cd535004b1fc8322d560f5b09463b248c81df0}"
NAME="${FIREFLY_CONTAINER:-firefly-ci}"

case "${1:-}" in
  start)
    main='java -jar target/firefly-*.jar'
    env=(-e SPRING_DOCKER_COMPOSE_ENABLED=false)
    if [[ "${2:-}" == "--plugins" ]]; then
      main='java -cp target/firefly-*.jar org.springframework.boot.loader.launch.PropertiesLauncher'
      env+=(-e LOADER_PATH=plugins)
    fi
    mkdir -p "$HOME/.firefly"
    read -r -a extra <<< "${FIREFLY_DOCKER_ARGS:-}"
    docker rm -f "$NAME" >/dev/null 2>&1 || true
    docker run -d --name "$NAME" --network host \
      -u "$(id -u):$(id -g)" -e HOME="$HOME" "${env[@]}" \
      -v "$ROOT_DIR:$ROOT_DIR" -v "$HOME/.firefly:$HOME/.firefly" -w "$ROOT_DIR" \
      "${extra[@]}" "$IMAGE" sh -c "exec $main" >/dev/null
    echo "started $NAME ($IMAGE)"
    ;;
  logs) docker logs "$NAME" 2>&1 ;;
  stop) docker rm -f "$NAME" >/dev/null 2>&1 || true ;;
  *) echo "usage: $0 start [--plugins] | logs | stop" >&2; exit 2 ;;
esac
