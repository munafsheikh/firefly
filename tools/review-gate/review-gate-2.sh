#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
export REVIEW_GATE_NAME="Review Gate 2"
export OLLAMA_MODEL="${REVIEW_GATE_2_MODEL:-qwen3.6:27b}"

exec bash "$SCRIPT_DIR/run-review-gate.sh" "$@"
