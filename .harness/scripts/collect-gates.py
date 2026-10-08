#!/usr/bin/env python3
"""Deterministic gate collector. Runs the toolchain once and writes AUTO check results.

Writes:  .harness/output/checks.env       (A1..A4, T1, T2, T5 = PASS|FAIL)
         .harness/output/gate-details.txt (file:line evidence for failures)
         .harness/output/gate-log.txt     (raw maven output)
"""
import glob, os, re, subprocess, sys
import xml.etree.ElementTree as ET

OUT = ".harness/output"
os.makedirs(OUT, exist_ok=True)
res, details = {}, []

def run(cmd):
    try:
        p = subprocess.run(cmd, capture_output=True, text=True)
        return p.returncode, p.stdout + p.stderr
    except FileNotFoundError:
        return 127, f"command not found: {cmd[0]}"

mvn = "./mvnw" if os.path.exists("mvnw") else "mvn"
rc, log = run([mvn, "-B", "-fae", "verify", "-Dcheckstyle.failOnViolation=false"])
open(f"{OUT}/gate-log.txt", "w").write(log)

# --- Checkstyle ---
cs_total, raw_throws = 0, 0
for f in glob.glob("target/checkstyle-result.xml"):
    for file_el in ET.parse(f).getroot().iter("file"):
        for err in file_el.iter("error"):
            cs_total += 1
            rel = os.path.relpath(file_el.get("name"))
            details.append(f"CHECKSTYLE {rel}:{err.get('line')} {err.get('message')}")
            if "RawThrow" in (err.get("message") or ""):
                raw_throws += 1

# --- ArchUnit via surefire XML ---
arch = {}
for f in glob.glob("target/surefire-reports/TEST-*ArchitectureTest.xml"):
    for tc in ET.parse(f).getroot().iter("testcase"):
        m = re.match(r"(A\d)_", tc.get("name"))
        if not m:
            continue
        bad = tc.find("failure") is not None or tc.find("error") is not None
        arch.setdefault(m.group(1), []).append(not bad)
        if bad:
            el = tc.find("failure") if tc.find("failure") is not None else tc.find("error")
            details.append(f"ARCHUNIT {tc.get('name')}: {(el.get('message') or '')[:600]}")

def arch_ok(k):
    return bool(arch.get(k)) and all(arch[k])

res["A1"] = arch_ok("A1")
res["A2"] = arch_ok("A2") and raw_throws == 0
res["A3"] = arch_ok("A3")
res["A4"] = arch_ok("A4")
res["T1"] = rc == 0 and cs_total == 0
res["T2"] = "All coverage checks have been met" in log
disabled = []
for f in glob.glob("src/test/**/*.java", recursive=True):
    for i, line in enumerate(open(f), 1):
        if re.search(r"@Disabled|@Ignore", line):
            disabled.append(f"{f}:{i}")
res["T5"] = not disabled
details += [f"DISABLED_TEST {d}" for d in disabled]
if "Coverage checks have not been met" in log:
    details += [l.strip() for l in log.splitlines() if "Rule violated" in l]
if rc != 0:
    details += [l.strip() for l in log.splitlines() if l.startswith("[ERROR]")][:25]

with open(f"{OUT}/checks.env", "w") as fh:
    for k in sorted(res):
        fh.write(f"{k}={'PASS' if res[k] else 'FAIL'}\n")
open(f"{OUT}/gate-details.txt", "w").write("\n".join(details) + "\n")
print(open(f"{OUT}/checks.env").read())
sys.exit(0)
