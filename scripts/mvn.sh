#!/usr/bin/env bash
# Docker-first Maven: run any Maven command for any module inside the Firefly build image
# (.docker/build/Dockerfile: pinned JDK 25 + Maven + Playwright OS libraries), so builds and
# tests behave the same on every machine and CI runner. The only host requirement is Docker.
# The image is built on first use and tagged by a hash of its Dockerfile, so it is rebuilt
# only when that file changes.
#
#   scripts/mvn.sh [-C <module-dir>] <maven args...>
#   scripts/mvn.sh -B -V clean verify
#   scripts/mvn.sh -C plugins/cli-plugin -B clean package
#
# Environment:
#   FIREFLY_BUILD_IMAGE   use this image instead of building .docker/build/Dockerfile
#   FIREFLY_IMAGE_BUILD_ARGS  extra `docker build` flags (e.g. proxy build args, --secret id=ca,...)
#   FIREFLY_M2_DIR        host Maven repository cache, bind-mounted (default: ~/.m2)
#   FIREFLY_DOCKER_ARGS   extra `docker run` flags (e.g. proxy env, CA bundle mounts)
#   FIREFLY_LOCAL=1       run the host's mvn instead of Docker (escape hatch)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
M2_DIR="${FIREFLY_M2_DIR:-$HOME/.m2}"

module="."
if [[ "${1:-}" == "-C" ]]; then
  module="$2"
  shift 2
fi

if [[ "${FIREFLY_LOCAL:-0}" == "1" ]]; then
  cd "$ROOT_DIR/$module" && exec mvn "$@"
fi

IMAGE="${FIREFLY_BUILD_IMAGE:-}"
if [[ -z "$IMAGE" ]]; then
  IMAGE="firefly-build:$(sha256sum "$ROOT_DIR/.docker/build/Dockerfile" | cut -c1-12)"
  if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
    read -r -a build_args <<< "${FIREFLY_IMAGE_BUILD_ARGS:-}"
    echo "[mvn.sh] building $IMAGE" >&2
    docker build -q "${build_args[@]}" -t "$IMAGE" "$ROOT_DIR/.docker/build" >&2
  fi
fi

mkdir -p "$M2_DIR/repository"
read -r -a extra <<< "${FIREFLY_DOCKER_ARGS:-}"
tty=()
[[ -t 0 && -t 1 ]] && tty=(-t)

exec docker run --rm -i "${tty[@]}" \
  -u "$(id -u):$(id -g)" \
  -e HOME=/tmp -e MAVEN_CONFIG=/tmp/.m2 \
  -v "$ROOT_DIR:/workspace" \
  -v "$M2_DIR:/tmp/.m2" \
  -w "/workspace/$module" \
  "${extra[@]}" \
  "$IMAGE" mvn -Dmaven.repo.local=/tmp/.m2/repository "$@"
