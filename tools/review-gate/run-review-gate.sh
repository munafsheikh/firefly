#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: run-review-gate.sh [--base <ref>] [--diff-file <path>] [--allow-pull]

Runs ReviewGate.java against a Java diff using a locally served Ollama model.
By default no network/model download is permitted. Preload the configured model
before running locally, or pass --allow-pull explicitly (CI Gate 1 only).

Environment:
  OLLAMA_HOST             Ollama URL (default: http://localhost:11434)
  OLLAMA_MODEL            Required model name
  REVIEW_GATE_NAME        Display name for diagnostics
  REVIEW_BASE_REF         Base branch/ref (default: GITHUB_BASE_REF or main)
  REVIEW_DIFF_FILE        Existing diff to review instead of generating one
  REVIEW_GATE_ALLOW_PULL  true to allow Ollama model pull; default false
USAGE
}

GATE_NAME="${REVIEW_GATE_NAME:-Review Gate}"
OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"
MODEL="${OLLAMA_MODEL:-}"
BASE_REF="${REVIEW_BASE_REF:-${GITHUB_BASE_REF:-main}}"
DIFF_FILE="${REVIEW_DIFF_FILE:-}"
ALLOW_PULL="${REVIEW_GATE_ALLOW_PULL:-false}"

while (($#)); do
  case "$1" in
    --base)
      [[ $# -ge 2 ]] || { echo "[$GATE_NAME] --base requires a value" >&2; exit 2; }
      BASE_REF="$2"
      shift 2
      ;;
    --diff-file)
      [[ $# -ge 2 ]] || { echo "[$GATE_NAME] --diff-file requires a value" >&2; exit 2; }
      DIFF_FILE="$2"
      shift 2
      ;;
    --allow-pull)
      ALLOW_PULL=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[$GATE_NAME] Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

[[ -n "$MODEL" ]] || { echo "[$GATE_NAME] OLLAMA_MODEL is required." >&2; exit 2; }

for cmd in git curl java; do
  command -v "$cmd" >/dev/null 2>&1 || {
    echo "[$GATE_NAME] Required command not found: $cmd" >&2
    exit 2
  }
done

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "[$GATE_NAME] Must be run inside the Firefly git repository." >&2
  exit 2
}
cd "$REPO_ROOT"

REVIEW_GATE_SOURCE="$REPO_ROOT/tools/review-gate/ReviewGate.java"
[[ -f "$REVIEW_GATE_SOURCE" ]] || {
  echo "[$GATE_NAME] Missing $REVIEW_GATE_SOURCE" >&2
  exit 2
}

echo "[$GATE_NAME] model=$MODEL host=$OLLAMA_HOST"

GENERATED_DIFF=""
cleanup() {
  [[ -z "$GENERATED_DIFF" ]] || rm -f "$GENERATED_DIFF"
}
trap cleanup EXIT

if ! curl --silent --show-error --fail "${OLLAMA_HOST}/api/tags" >/dev/null; then
  echo "[$GATE_NAME] Cannot reach local Ollama at ${OLLAMA_HOST}." >&2
  exit 2
fi

if ! curl --silent --show-error --fail "${OLLAMA_HOST}/api/show" \
    -H 'Content-Type: application/json' \
    -d "{\"model\":\"${MODEL}\"}" >/dev/null 2>&1; then
  if [[ "$ALLOW_PULL" == "true" ]]; then
    echo "[$GATE_NAME] $MODEL is not present; pulling because REVIEW_GATE_ALLOW_PULL=true."
    curl --silent --show-error --fail "${OLLAMA_HOST}/api/pull" \
      -H 'Content-Type: application/json' \
      -d "{\"name\":\"${MODEL}\",\"stream\":false}" >/dev/null
  else
    echo "[$GATE_NAME] $MODEL is not installed locally." >&2
    echo "[$GATE_NAME] Offline mode forbids downloads. Preload once with: ollama pull $MODEL" >&2
    exit 2
  fi
fi

if [[ -n "$DIFF_FILE" ]]; then
  [[ -f "$DIFF_FILE" ]] || { echo "[$GATE_NAME] Diff file not found: $DIFF_FILE" >&2; exit 2; }
  REVIEW_DIFF="$DIFF_FILE"
else
  if git rev-parse --verify --quiet "$BASE_REF^{commit}" >/dev/null; then
    RESOLVED_BASE="$BASE_REF"
  elif git rev-parse --verify --quiet "origin/$BASE_REF^{commit}" >/dev/null; then
    RESOLVED_BASE="origin/$BASE_REF"
  else
    echo "[$GATE_NAME] Base ref '$BASE_REF' is not available locally." >&2
    echo "[$GATE_NAME] Offline mode does not fetch refs; fetch it before disconnecting or pass --base <local-ref>." >&2
    exit 2
  fi

  MERGE_BASE="$(git merge-base "$RESOLVED_BASE" HEAD)"
  GENERATED_DIFF="$(mktemp)"
  REVIEW_DIFF="$GENERATED_DIFF"

  # Compare the merge base with the working tree. This includes committed,
  # staged, and unstaged Java changes, which makes the same runner useful
  # before a PR is created as well as in a clean CI checkout.
  git diff --no-ext-diff --binary "$MERGE_BASE" -- '*.java' > "$REVIEW_DIFF"

  # git diff does not include untracked files. Add new untracked Java files
  # as /dev/null -> file patches so pre-PR local review does not miss them.
  status=0
  while IFS= read -r -d '' file; do
    git diff --no-ext-diff --binary --no-index -- /dev/null "$file" >> "$REVIEW_DIFF" || status=$?
    if [[ "$status" -gt 1 ]]; then
      echo "[$GATE_NAME] Failed to diff untracked file: $file" >&2
      exit 2
    fi
    status=0
  done < <(git ls-files --others --exclude-standard -z -- '*.java')
fi

DIFF_LINES="$(wc -l < "$REVIEW_DIFF" | tr -d ' ')"
echo "[$GATE_NAME] Java diff lines: $DIFF_LINES"

java "$REVIEW_GATE_SOURCE" --model "$MODEL" < "$REVIEW_DIFF"
