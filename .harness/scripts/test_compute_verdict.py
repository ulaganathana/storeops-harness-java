#!/usr/bin/env python3
"""Regression tests for deterministic score calculation and hard gates."""

import csv
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHECKS = ROOT / ".harness" / "eval" / "checks.csv"
SCRIPT = ROOT / ".harness" / "scripts" / "compute-verdict.py"


class VerdictTests(unittest.TestCase):
    def setUp(self):
        with CHECKS.open(newline="", encoding="utf-8") as stream:
            self.checks = list(csv.DictReader(stream))
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.results_path = Path(self.temporary_directory.name) / "results.csv"

    def tearDown(self):
        self.temporary_directory.cleanup()

    def write_results(self, overrides=None, missing_evidence=None):
        overrides = overrides or {}
        missing_evidence = missing_evidence or set()
        with self.results_path.open("w", newline="", encoding="utf-8") as stream:
            writer = csv.writer(stream)
            writer.writerow(["id", "result", "evidence"])
            for check in self.checks:
                check_id = check["id"]
                writer.writerow([
                    check_id,
                    overrides.get(check_id, "PASS"),
                    "" if check_id in missing_evidence else f"src/test/{check_id}.java:1",
                ])

    def verdict(self):
        return subprocess.run(
            [sys.executable, str(SCRIPT), str(self.results_path)],
            cwd=ROOT,
            check=False,
            capture_output=True,
            text=True,
        )

    def test_identical_results_produce_identical_pass_verdict(self):
        self.write_results()

        first = self.verdict()
        second = self.verdict()

        self.assertEqual(0, first.returncode)
        self.assertEqual(first.stdout, second.stdout)
        self.assertIn("VERDICT: PASS", first.stdout)

    def test_failed_hard_gate_forces_fail_despite_other_passes(self):
        self.write_results({"A1": "FAIL"})

        result = self.verdict()

        self.assertEqual(0, result.returncode)
        self.assertIn("HARD_GATE_FAILURES: A1=FAIL", result.stdout)
        self.assertIn("VERDICT: FAIL", result.stdout)

    def test_pass_without_evidence_is_rejected(self):
        self.write_results(missing_evidence={"T1"})

        result = self.verdict()

        self.assertNotEqual(0, result.returncode)
        self.assertIn("requires evidence", result.stderr)


if __name__ == "__main__":
    unittest.main()
