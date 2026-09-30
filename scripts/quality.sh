#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAVEN="${MAVEN:-$ROOT_DIR/mvnw}"
JACOCO_VERSION="${JACOCO_VERSION:-0.8.15}"
PITEST_VERSION="${PITEST_VERSION:-1.25.9}"

MODULES=(
  "."
  "mcp-embedded"
  "plugins/actuator-plugin"
  "plugins/ado-plugin"
  "plugins/bash-runner-plugin"
  "plugins/cli-plugin"
  "plugins/markdown-plugin"
  "plugins/mcp-registry-plugin"
  "plugins/midnight-theme-plugin"
  "plugins/plantuml-plugin"
  "plugins/webtui-plugin"
)

usage() {
  cat <<'EOF'
Usage: ./scripts/quality.sh <coverage|mutation>

coverage  Run tests for every Maven module with JaCoCo instrumentation and XML/HTML reports.
mutation  Run PIT mutation testing for modules that contain Java tests.

Environment overrides:
  MAVEN=/path/to/mvn
  JACOCO_VERSION=0.8.15
  PITEST_VERSION=1.25.9
EOF
}

module_name() {
  if [[ "$1" == "." ]]; then
    printf '%s\n' "root"
  else
    printf '%s\n' "$1"
  fi
}

run_coverage() {
  for module in "${MODULES[@]}"; do
    local pom="$ROOT_DIR/$module/pom.xml"
    [[ -f "$pom" ]] || continue
    echo "::group::JaCoCo coverage — $(module_name "$module")"
    "$MAVEN" -f "$pom" -B -V clean \
      "org.jacoco:jacoco-maven-plugin:${JACOCO_VERSION}:prepare-agent" \
      test \
      "org.jacoco:jacoco-maven-plugin:${JACOCO_VERSION}:report"
    echo "::endgroup::"
  done
}

has_java_tests() {
  local test_dir="$ROOT_DIR/$1/src/test/java"
  [[ -d "$test_dir" ]] && find "$test_dir" -type f -name '*.java' -print -quit | grep -q .
}

run_mutation() {
  for module in "${MODULES[@]}"; do
    local pom="$ROOT_DIR/$module/pom.xml"
    [[ -f "$pom" ]] || continue

    if ! has_java_tests "$module"; then
      echo "Skipping $(module_name "$module"): no Java tests"
      continue
    fi

    echo "::group::PIT mutation coverage — $(module_name "$module")"
    "$MAVEN" -f "$pom" -B -V test-compile \
      "org.pitest:pitest-maven:${PITEST_VERSION}:mutationCoverage" \
      -DoutputFormats=XML,HTML \
      -DtimestampedReports=false \
      -DwithHistory=true \
      -DfailWhenNoMutations=false \
      -Dthreads=2
    echo "::endgroup::"
  done
}

case "${1:-}" in
  coverage) run_coverage ;;
  mutation) run_mutation ;;
  *) usage; exit 2 ;;
esac
