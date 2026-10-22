#!/usr/bin/env python3
"""Compute the fixed harness score from CSV gate results; no LLM arithmetic."""

import csv
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHECKS_FILE = ROOT / ".harness" / "eval" / "checks.csv"
WEIGHTS_FILE = ROOT / ".harness" / "eval" / "weights.json"


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: python .harness/scripts/compute-verdict.py <results.csv>")

    with CHECKS_FILE.open(newline="", encoding="utf-8") as stream:
        check_rows = list(csv.DictReader(stream))
    checks = {row["id"]: row for row in check_rows}
    if len(checks) != len(check_rows):
        raise SystemExit("check configuration contains duplicate IDs")
    with WEIGHTS_FILE.open(encoding="utf-8") as stream:
        config = json.load(stream)
    dimensions = config["dimensions"]
    if sum(item["weight"] for item in dimensions.values()) != 100:
        raise SystemExit("dimension weights must sum to 100")

    with Path(sys.argv[1]).open(newline="", encoding="utf-8") as stream:
        result_rows = list(csv.DictReader(stream))
    results = {row["id"]: row for row in result_rows}
    if len(results) != len(result_rows):
        raise SystemExit("results contain duplicate check IDs")
    if set(results) != set(checks):
        raise SystemExit("results must contain exactly the configured check IDs")

    hard_failures = []
    weighted_score = 0.0
    for dimension, settings in dimensions.items():
        dimension_checks = [check for check in checks.values()
                            if check["dimension"] == dimension]
        if not dimension_checks:
            raise SystemExit(f"dimension {dimension} has no checks")
        passed = 0
        for check in dimension_checks:
            result = results[check["id"]]
            state = result["result"].strip().upper()
            if state not in {"PASS", "FAIL", "BLOCKED"}:
                raise SystemExit(f"invalid result for {check['id']}: {state}")
            if state == "PASS" and not result.get("evidence", "").strip():
                raise SystemExit(f"passing check {check['id']} requires evidence")
            if state == "PASS":
                passed += 1
            if check["hard"].lower() == "true" and state != "PASS":
                hard_failures.append(f"{check['id']}={state}")
        weighted_score += settings["weight"] * passed / len(dimension_checks)

    score = round(weighted_score, 2)
    if hard_failures or score < config["conditionalThreshold"]:
        verdict = "FAIL"
    elif score < config["passThreshold"]:
        verdict = "CONDITIONAL PASS"
    else:
        verdict = "PASS"

    print(f"SCORE: {score:.2f}")
    print("HARD_GATE_FAILURES: " + (", ".join(hard_failures) if hard_failures else "NONE"))
    print(f"VERDICT: {verdict}")


if __name__ == "__main__":
    main()
