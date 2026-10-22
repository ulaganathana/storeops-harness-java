#!/usr/bin/env python3
"""Fail when a test source contains a disabled or ignored test annotation."""

import re
from pathlib import Path

root = Path(__file__).resolve().parents[2] / "src" / "test"
annotations = re.compile(
    r"@\s*(?:org\.junit\.(?:jupiter\.)?api\.)?(?:Disabled|Ignore)\b"
)
violations = []
for source in root.rglob("*.java"):
    for line_number, line in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
        if annotations.search(line):
            violations.append(f"{source}:{line_number}")

if violations:
    print("Disabled or ignored tests found:")
    print("\n".join(violations))
    raise SystemExit(1)

print("PASS: no disabled or ignored test annotations found")
