#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
export REVIEW_GATE_NAME="Review Gate 1"
export OLLAMA_MODEL="${REVIEW_GATE_1_MODEL:-qwen2.5-coder:1.5b}"

exec bash "$SCRIPT_DIR/run-review-gate.sh" "$@"
