#!/usr/bin/env python3
from __future__ import annotations

import glob
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def module_from_path(path: Path) -> str:
    rel = path.relative_to(ROOT)
    parts = rel.parts
    if parts[0] == "target":
        return "root"
    if parts[0] == "plugins":
        return "/".join(parts[:2])
    return parts[0]


def percent(numerator: int, denominator: int) -> str:
    return "n/a" if denominator == 0 else f"{100.0 * numerator / denominator:.1f}%"


def write_summary(text: str) -> None:
    summary = os.getenv("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text)
            if not text.endswith("\n"):
                handle.write("\n")
    print(text)


def coverage_report() -> None:
    files = [Path(p) for p in glob.glob(str(ROOT / "**/target/site/jacoco/jacoco.xml"), recursive=True)]
    rows = []
    total_covered = total_missed = 0

    for path in sorted(files):
        root = ET.parse(path).getroot()
        counter = next((c for c in root.findall("counter") if c.get("type") == "LINE"), None)
        if counter is None:
            continue
        covered = int(counter.get("covered", "0"))
        missed = int(counter.get("missed", "0"))
        rows.append((module_from_path(path), covered, missed, percent(covered, covered + missed)))
        total_covered += covered
        total_missed += missed

    lines = ["## JaCoCo coverage", "", "| Module | Covered lines | Missed lines | Line coverage |", "|---|---:|---:|---:|"]
    for module, covered, missed, score in rows:
        lines.append(f"| {module} | {covered} | {missed} | {score} |")
    lines.append(f"| **Total** | **{total_covered}** | **{total_missed}** | **{percent(total_covered, total_covered + total_missed)}** |")
    if not rows:
        lines.append("\n_No JaCoCo XML reports were found._")
    write_summary("\n".join(lines) + "\n")


def mutation_report() -> None:
    files = [Path(p) for p in glob.glob(str(ROOT / "**/target/pit-reports/mutations.xml"), recursive=True)]
    rows = []
    totals = {"KILLED": 0, "SURVIVED": 0, "NO_COVERAGE": 0, "OTHER": 0}

    for path in sorted(files):
        counts = {"KILLED": 0, "SURVIVED": 0, "NO_COVERAGE": 0, "OTHER": 0}
        root = ET.parse(path).getroot()
        for mutation in root.findall("mutation"):
            status = mutation.get("status", "OTHER")
            key = status if status in counts else "OTHER"
            counts[key] += 1
            totals[key] += 1
        considered = counts["KILLED"] + counts["SURVIVED"] + counts["NO_COVERAGE"] + counts["OTHER"]
        rows.append((module_from_path(path), counts, considered))

    total_mutants = sum(totals.values())
    lines = [
        "## PIT mutation coverage",
        "",
        "Mutation score below is intentionally reported as killed / all generated mutants; no enforcement threshold is applied until a baseline is established.",
        "",
        "| Module | Killed | Survived | No coverage | Other | Mutation score |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for module, counts, considered in rows:
        lines.append(
            f"| {module} | {counts['KILLED']} | {counts['SURVIVED']} | {counts['NO_COVERAGE']} | {counts['OTHER']} | {percent(counts['KILLED'], considered)} |"
        )
    lines.append(
        f"| **Total** | **{totals['KILLED']}** | **{totals['SURVIVED']}** | **{totals['NO_COVERAGE']}** | **{totals['OTHER']}** | **{percent(totals['KILLED'], total_mutants)}** |"
    )
    if not rows:
        lines.append("\n_No PIT XML reports were found._")
    write_summary("\n".join(lines) + "\n")


def main() -> int:
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "coverage":
        coverage_report()
        return 0
    if mode == "mutation":
        mutation_report()
        return 0
    print("usage: quality-report.py <coverage|mutation>", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
