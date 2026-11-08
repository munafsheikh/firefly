"""Assertions for the review-gate end-to-end test.

  python assert_review.py <review.json> <exit-code> <expected model ref> <expected rule ids...>

Checks that the review ran through the model (not degraded to rules-only), that the model's
output was used, that deterministic rules caught the fixture's known defects, and that the
gate blocked. Prints a short report; exits non-zero on the first failed expectation.
"""
import json
import sys


def main(path, exit_code, model_ref, *rules):
    r = json.load(open(path, encoding="utf-8"))
    run, findings = r["run"], r["findings"]
    checks = []

    def check(name, ok, detail=""):
        checks.append((name, ok, detail))

    check("model runtime used (not degraded)", not run["degraded"], "; ".join(run["infrastructure_errors"]))
    check("expected model routed", any(m["ref"] == model_ref for m in run["models"]),
          ", ".join(m["ref"] for m in run["models"]) or "no model")
    check("model calls succeeded", run["calls"]["total"] >= 1 and run["calls"]["failed"] == 0, json.dumps(run["calls"]))
    parse_errors = [p["parse_error"] for p in run.get("parse", []) if p.get("parse_error")]
    check("model output parsed", run.get("parse") and not parse_errors, "; ".join(parse_errors))
    found = {f["rule_id"] for f in findings}
    missing = [x for x in rules if x not in found]
    check("deterministic rules caught the fixture defects", not missing, f"missing {missing}" if missing else "")
    check("gate blocked the change", r["gate"]["status"] == "block" and exit_code == "1",
          f"status={r['gate']['status']} exit={exit_code}")

    model_findings = [f for f in findings if f["source"] == "model"]
    corroborated = [f for f in findings if f["corroborated_by"]]
    print(f"### Review gate end-to-end\n\nModel `{model_ref}` · {len(findings)} findings "
          f"({len(model_findings)} from the model, {len(corroborated)} rule findings corroborated by it)\n")
    print("| Check | Result | Detail |\n|---|---|---|")
    for name, ok, detail in checks:
        print(f"| {name} | {'pass' if ok else 'FAIL'} | {detail} |")
    if model_findings:
        print("\n**Model findings**\n")
        for f in model_findings:
            print(f"- `{f['location']['path']}:{f['location']['start_line']}` [{f['severity']}, {f['confidence']}] {f['title']}")
    return 0 if all(ok for _, ok, _ in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main(*sys.argv[1:]))
