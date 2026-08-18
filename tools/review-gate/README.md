# Local SLM Review Gates

Firefly has two reusable Java review gates. GitHub Actions and local Maven runs invoke the same Bash entrypoints.

| Gate | Default model | CI runner | Repository variable |
|---|---|---|---|
| Review Gate 1 | `qwen2.5-coder:1.5b` | GitHub-hosted CPU | `ENABLE_REVIEW_GATE_1=true` |
| Review Gate 2 | `qwen3.6:27b` | self-hosted `linux,gpu` | `ENABLE_REVIEW_GATE_2=true` |

The model names can be overridden with repository variables `REVIEW_GATE_1_MODEL` and `REVIEW_GATE_2_MODEL`, or locally with environment variables of the same names.

## What the gate reviews

`ReviewGate.java` performs a focused Java diff review for null-safety, resource leaks, SQL injection, hardcoded secrets/credentials, obvious concurrency bugs, and swallowed exceptions. It is an AI review layer, not a replacement for compilation, unit tests, integration tests, static analysis, or human review.

Large diffs are never silently truncated. The full diff is split into bounded chunks (24,000 characters by default), every chunk is reviewed, and the worst severity is aggregated across all chunks. Override the chunk size with `REVIEW_GATE_CHUNK_CHARS` when needed.

The model response is fail-closed. Every chunk must contain exactly one valid `SEVERITY: NONE|MINOR|BLOCK` line and a non-empty `SUMMARY:` line. Malformed output, missing fields, Ollama protocol errors, inference failures, or incomplete review execution return exit code `2` and fail an enabled CI gate.

## Deterministic self-tests

The gate implementation has dependency-free tests that do not call Ollama or the network:

```bash
bash tools/review-gate/test-review-gate.sh
```

They cover clean/minor/block responses, malformed and duplicate severity output, missing summaries, simulated inference failure, lossless large-diff chunking, worst-severity aggregation, malformed later chunks, and strict Ollama JSON parsing. Both enabled CI gates run these tests before invoking their model.

## One-time local preparation

Run Ollama locally and download the models while you still have network access:

```bash
ollama pull qwen2.5-coder:1.5b
ollama pull qwen3.6:27b
```

Also make sure Maven/the `exec-maven-plugin` has already been resolved at least once before disconnecting. The review commands themselves use Maven offline mode and the Bash runner refuses to download a missing model.

## Manual pre-PR review, fully offline

From the repository root:

```bash
./mvnw -o exec:exec@review-gate-1
./mvnw -o exec:exec@review-gate-2
```

Both compare Java changes against `main` by default and include committed, staged, unstaged, and untracked `.java` files.

Use a different local base ref with:

```bash
./mvnw -o exec:exec@review-gate-1 -Dreview.base=development
./mvnw -o exec:exec@review-gate-2 -Dreview.base=development
```

Run both sequentially with:

```bash
./mvnw -o exec:exec@review-gate-1 exec:exec@review-gate-2
```

The Bash entrypoints can also be used directly:

```bash
bash tools/review-gate/review-gate-1.sh --base main
bash tools/review-gate/review-gate-2.sh --base main
```

## CI activation

The workflow jobs do not run unless their repository Actions variables exist with the literal value `true`:

```text
ENABLE_REVIEW_GATE_1=true
ENABLE_REVIEW_GATE_2=true
```

Gate 2 depends on Gate 1 when Gate 1 is enabled. If Gate 1 is disabled, Gate 2 can still run. Gate 2 will not run after a Gate 1 failure.

Gate 1 runs an ephemeral Ollama service and is explicitly allowed to pull its small model in CI. Gate 2 never downloads model weights during the PR pipeline; its self-hosted GPU runner must already have the configured model installed.

Exit codes are fail-closed when a gate is enabled:

- `0` — review completed with `NONE` or `MINOR`
- `1` — at least one reviewed chunk returned `BLOCK`
- `2` — the gate could not complete reliably, for example malformed model output, Ollama/model/base ref unavailable, HTTP/protocol failure, or interrupted inference
