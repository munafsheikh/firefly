#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

for cmd in javac java; do
  command -v "$cmd" >/dev/null 2>&1 || {
    echo "[ReviewGate tests] Required command not found: $cmd" >&2
    exit 2
  }
done

javac -d "$BUILD_DIR" \
  "$SCRIPT_DIR/ReviewGate.java" \
  "$SCRIPT_DIR/ReviewGateTest.java"

java -ea -cp "$BUILD_DIR" ReviewGateTest
