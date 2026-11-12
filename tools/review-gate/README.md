# Review gate

Reviews are performed by [ai-code-reviewers](https://github.com/munafsheikh/ai-code-reviewers)
(rules + local model, no external API), pinned to
[v0.2.0](https://github.com/munafsheikh/ai-code-reviewers/tree/v0.2.0) by commit SHA. This directory holds only the local wrapper.

- CI: `.github/workflows/review-gate.yml` (uses the platform's GitHub action): deterministic rules plus a
  local SLM (Ollama + `qwen2.5-coder:1.5b` on the CPU runner). Override with repository variables
  `LLM_RUNTIME_IMAGE`, `LLM_MODEL`, or `LLM_PROFILE` (`offline` = rules only).
- Local / pre-commit: `git diff --cached | tools/review-gate/review-gate.sh`
  - `ACR_PROFILE=legacy LLM_MODEL=qwen2.5-coder:1.5b` reproduces the former ReviewGate.java output and exit codes.
  - `ACR_HOME=/path/to/ai-code-reviewers` runs from a local checkout instead of the container image.

Exit codes: 0 pass/warn, 1 blocked, 2 review could not run.

## End-to-end test

`.github/workflows/review-gate-e2e.yml` runs the gate against `e2e/InvoiceRepository.java.fixture`
with the pinned test configuration in `e2e/test.env` (`LLM_RUNTIME_IMAGE`, `LLM_MODEL`,
`LLM_PROFILE`, `EXPECTED_RULES`). It asserts the model ran and its output was used, the rules
caught the fixture's known defects, and the gate blocked. Runs on changes to the gate, weekly,
and on demand (Actions > Review Gate E2E > Run workflow).

