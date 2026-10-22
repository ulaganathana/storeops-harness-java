#!/usr/bin/env python3
"""Run deterministic harness gates and write their CSV results."""

import csv
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / ".harness" / "output" / "check-results.csv"
PYTHON = sys.executable


def maven_command():
    configured = os.environ.get("MAVEN_CMD")
    if configured:
        if configured.lower().endswith((".cmd", ".bat")):
            return [os.environ.get("COMSPEC", "cmd.exe"), "/c", configured]
        return [configured]
    executable = shutil.which("mvn") or shutil.which("mvn.cmd")
    if executable:
        if executable.lower().endswith((".cmd", ".bat")):
            return [os.environ.get("COMSPEC", "cmd.exe"), "/c", executable]
        return [executable]
    temporary_maven = Path(os.environ.get("TEMP", "")) / "apache-maven-3.9.9" / "bin" / "mvn.cmd"
    if temporary_maven.is_file():
        return [os.environ.get("COMSPEC", "cmd.exe"), "/c", str(temporary_maven)]
    return ["mvn"]


def run_gate(command, description):
    print(f"RUN: {' '.join(command)}")
    try:
        completed = subprocess.run(
            command,
            cwd=ROOT,
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
    except FileNotFoundError as error:
        print(f"BLOCKED: {description}: {error}")
        return "BLOCKED", f"{description}: executable unavailable"

    log_path = ROOT / ".harness" / "output" / f"{description}.log"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.write_text(completed.stdout, encoding="utf-8")
    result = "PASS" if completed.returncode == 0 else "FAIL"
    print(f"{result}: {description} (exit {completed.returncode}; {log_path.relative_to(ROOT)})")
    return result, str(log_path.relative_to(ROOT))


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    results = {}

    mvn = maven_command()
    test_result, test_evidence = run_gate(mvn + ["-B", "test"], "maven-test")
    verify_result, verify_evidence = run_gate(mvn + ["-B", "verify"], "maven-verify")
    hygiene_result, hygiene_evidence = run_gate(
        [PYTHON, ".harness/scripts/verify-disabled-tests.py"], "disabled-test-check"
    )

    results["A1"] = (verify_result, verify_evidence)
    results["A2"] = (verify_result, verify_evidence)
    results["A3"] = ("BLOCKED", "LLM review evidence not supplied by gate collector")
    results["A4"] = ("BLOCKED", "LLM review evidence not supplied by gate collector")
    results["T1"] = (test_result, test_evidence)
    results["T2"] = (verify_result, verify_evidence)
    results["T3"] = ("BLOCKED", "LLM AC-to-business assertion evidence not supplied")
    results["T4"] = ("BLOCKED", "LLM service-error branch review not supplied")
    results["T5"] = (hygiene_result, hygiene_evidence)
    results["T6"] = ("BLOCKED", "Generator summary and AC traceability not supplied")

    with OUTPUT.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream)
        writer.writerow(["id", "result", "evidence"])
        for check_id in ("A1", "A2", "A3", "A4", "T1", "T2", "T3", "T4", "T5", "T6"):
            writer.writerow([check_id, *results[check_id]])
    print(f"Gate results written to {OUTPUT.relative_to(ROOT)}")
    return 1 if any(state != "PASS" for state, _ in results.values()) else 0


if __name__ == "__main__":
    raise SystemExit(main())
