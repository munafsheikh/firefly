# Review gate

Reviews are performed by [ai-code-reviewers](https://github.com/munafsheikh/ai-code-reviewers)
(rules + local model, no external API). This directory holds only the local wrapper.

- CI: `.github/workflows/review-gate.yml` (uses the platform's GitHub action; rules-only until
  repository variable `LLM_RUNTIME_IMAGE` is set; `LLM_MODEL` selects the model).
- Local / pre-commit: `git diff --cached | tools/review-gate/review-gate.sh`
  - `ACR_PROFILE=legacy LLM_MODEL=qwen2.5-coder:1.5b` reproduces the former ReviewGate.java output and exit codes.
  - `ACR_HOME=/path/to/ai-code-reviewers` runs from a local checkout instead of the container image.

Exit codes: 0 pass/warn, 1 blocked, 2 review could not run.
