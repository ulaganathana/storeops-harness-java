#!/usr/bin/env bash
# Submission readiness checker: maps every requirement in the capstone PDF (sections 5, 6, 7, 8) to a deterministic check.
# Usage: .harness/scripts/verify-submission.sh        exit 0 only if every FAIL-level check passes.
# Sections: [H] harness design (static)  [R] real demonstration-run evidence  [D] documents
cd "$(dirname "$0")/../.." || exit 2
pass=0; fail=0; warn=0
ok()   { printf "  PASS  %s\n" "$1"; pass=$((pass+1)); }
no()   { printf "  FAIL  %s\n" "$1"; fail=$((fail+1)); }
wn()   { printf "  WARN  %s\n" "$1"; warn=$((warn+1)); }
chk()  { if ( eval "$2" ) >/dev/null 2>&1; then ok "$1"; else no "$1"; fi; }
has()  { grep -qiE "$2" "$1" 2>/dev/null; }

echo "== [H] 5.1 CLAUDE.md orchestrator =="
chk "entry prompt format (@planner)"            "has CLAUDE.md '@planner'"
chk "agent files referenced"                    "has CLAUDE.md 'planner.agent.md' && has CLAUDE.md 'monitor.agent.md'"
chk "AWAITING APPROVAL -> APPROVED sequence"    "has CLAUDE.md 'AWAITING APPROVAL' && has CLAUDE.md 'APPROVED'"
chk "routing reads VERDICT line"                "has CLAUDE.md '\\^VERDICT'"
chk "max 3 iterations + escalation.md"          "has CLAUDE.md 'Maximum 3' && has CLAUDE.md 'escalation.md'"
chk "context scoping / reset strategy"          "has CLAUDE.md 'Context scoping' && has CLAUDE.md '/clear'"
chk "CI/CD relationship"                        "has CLAUDE.md 'CI/CD relationship'"
echo "== [H] 5.2 agent files =="
for a in planner generator evaluator monitor; do chk "$a.agent.md has Read-before-acting + Produce" "has .harness/agents/$a.agent.md 'Read before acting' && has .harness/agents/$a.agent.md 'Produce'"; done
chk "planner reads app-context+architecture-principles+sprint-decomposition" "for s in app-context architecture-principles sprint-decomposition; do has .harness/agents/planner.agent.md \$s || exit 1; done"
chk "evaluator reads architecture-principles+how-to-review+grading-criteria" "for s in architecture-principles how-to-review grading-criteria; do has .harness/agents/evaluator.agent.md \$s || exit 1; done"
echo "== [H] 5.3 skills (>=6; shared 2; generator >=2; evaluator >=2) =="
n=$(ls .harness/skills/*/SKILL.md 2>/dev/null | wc -l)
chk "skill count >= 6 (found $n)" "[ $n -ge 6 ]"
chk "shared: app-context + architecture-principles" "[ -f .harness/skills/app-context/SKILL.md ] && [ -f .harness/skills/architecture-principles/SKILL.md ]"
g=0; for s in coding-conventions how-to-test api-integration; do [ -f .harness/skills/$s/SKILL.md ] && g=$((g+1)); done
e=0; for s in how-to-review grading-criteria; do [ -f .harness/skills/$s/SKILL.md ] && e=$((e+1)); done
chk "generator-specific skills >= 2 (found $g)" "[ $g -ge 2 ]"
chk "evaluator-specific skills >= 2 (found $e)" "[ $e -ge 2 ]"
chk "skills are StoreOps-specific (mention EventBus, AppError, ArchUnit, JaCoCo)" "for k in EventBus AppError ArchUnit JaCoCo; do grep -rqi \$k .harness/skills || exit 1; done"
echo "== [H] 5.4 evaluation framework =="
. .harness/eval/weights.env 2>/dev/null
chk "dimension weights sum to 100 (${D1_WEIGHT:-?}+${D2_WEIGHT:-?})" "[ \$(( ${D1_WEIGHT:-0} + ${D2_WEIGHT:-0} )) -eq 100 ]"
for d in D1 D2; do chk "$d has >=1 hard AUTO gate (tool-backed)" "awk -F, -v d=$d '\$2==d && \$3==\"Y\" && \$4==\"AUTO\"{f=1} END{exit !f}' .harness/eval/checks.csv"; done
chk "LLM checks require evidence (script rule R2)" "grep -q '_EVIDENCE' .harness/scripts/compute-verdict.sh"
# determinism self-test with fixtures
tmp=$(mktemp); trap 'rm -f "$tmp" "$tmp.out1" "$tmp.out2"' EXIT
{ for k in A1 A2 A3 A4 T1 T2 T5; do echo "$k=PASS"; done; for k in A5 A6 A7 A8 T3 T4 T6; do echo "$k=PASS"; echo "${k}_EVIDENCE=x:1"; done; } > "$tmp"
bash .harness/scripts/compute-verdict.sh "$tmp" > "$tmp.out1"; bash .harness/scripts/compute-verdict.sh "$tmp" > "$tmp.out2"
chk "determinism: identical input -> identical output" "cmp -s $tmp.out1 $tmp.out2"
chk "all-pass fixture -> PASS 100"               "grep -q '^VERDICT: PASS' $tmp.out1 && grep -q '^SCORE: 100' $tmp.out1"
sed -i 's/^T3=PASS/T3=FAIL/' "$tmp"; bash .harness/scripts/compute-verdict.sh "$tmp" > "$tmp.out1"
chk "hard gate T3 fail overrides score -> FAIL"  "grep -q '^VERDICT: FAIL' $tmp.out1 && grep -q 'HARD_GATE_FAILURES: T3' $tmp.out1"
sed -i 's/^T3=FAIL/T3=PASS/;/^T3_EVIDENCE/d' "$tmp"; bash .harness/scripts/compute-verdict.sh "$tmp" > "$tmp.out1"
chk "PASS without evidence is rejected (R2)"     "grep -q '^VERDICT: FAIL' $tmp.out1"
echo "== [H] 8.2 integration / platform =="
chk "harness in .harness/, pipeline in .github/" "[ -d .harness ] && [ -f .github/workflows/ci.yml ]"
chk "monitor run-log fields defined"            "for k in SPRINT_ID VERDICT ITERATIONS_USED ESCALATION ESTIMATED_TOKENS; do has .harness/agents/monitor.agent.md \$k || exit 1; done"
chk ".harness/output ignored, reviews tracked"  "grep -q 'harness/output' .gitignore && ! grep -q 'harness/reviews' .gitignore"
echo "== [H] application baseline =="
chk "5 modules present (reports may be package-info only)" "for m in activities programmes staff alerts reports; do [ -d src/main/java/com/storeops/\$m ] || exit 1; done"
chk "9 base endpoints mapped"                   "[ \$(grep -rhoE '@(Get|Post|Patch|Delete)Mapping' src/main/java/com/storeops/{activities,programmes,alerts} | wc -l) -ge 9 ]"
chk "baseline commit exists"                    "git log --format=%s | grep -q '^baseline: generated StoreOps scaffold'"
chk "Dockerfile + compose present"              "[ -f Dockerfile ] && [ -f docker-compose.yml ]"

echo "== [R] 5.5/6.3 demonstration run evidence (must come from a REAL run) =="
R=.harness/reviews
chk "PROMPT.md present"                         "[ -s PROMPT.md ] && has PROMPT.md '^@planner'"
chk "spec.md committed in reviews/"             "git ls-files --error-unmatch $R/spec.md"
chk "sprint-1-contract.md committed"            "git ls-files --error-unmatch $R/sprint-1-contract.md"
chk "sprint-1-generator-summary.md committed"   "git ls-files --error-unmatch $R/sprint-1-generator-summary.md"
chk "sprint-1-evaluator-feedback.md committed"  "git ls-files --error-unmatch $R/sprint-1-evaluator-feedback.md"
chk "sprint-1-run-log.md committed"             "git ls-files --error-unmatch $R/sprint-1-run-log.md"
chk "spec.md references the feature prompt"     "has $R/spec.md 'FEATURE_PROMPT'"
chk "contract references the spec"              "has $R/sprint-1-contract.md 'SPEC:'"
chk "contract has GIVEN/WHEN/THEN"              "has $R/sprint-1-contract.md 'GIVEN' && has $R/sprint-1-contract.md 'WHEN' && has $R/sprint-1-contract.md 'THEN'"
chk "generator-summary covers every contract AC id" "grep -qE 'AC-1\\.[0-9]+' $R/sprint-1-contract.md && for ac in \$(grep -oE 'AC-1\\.[0-9]+' $R/sprint-1-contract.md | sort -u); do grep -q \$ac $R/sprint-1-generator-summary.md || exit 1; done"
chk "evaluator-feedback first line is a VERDICT" "head -1 $R/sprint-1-evaluator-feedback.md | grep -qE '^VERDICT: (PASS|CONDITIONAL PASS|FAIL)$'"
chk "evaluator-feedback cites file:line"        "grep -qE '[A-Za-z]+\\.java:[0-9]+' $R/sprint-1-evaluator-feedback.md"
chk "run-log has all required fields"           "for k in SPRINT_ID VERDICT ITERATIONS_USED ESCALATION ESTIMATED_TOKENS; do has $R/sprint-1-run-log.md \$k || exit 1; done"
chk "Generator changed src/ since baseline"     "[ -n \"\$(git diff --name-only \$(git log --format=%h --grep='^baseline: generated StoreOps scaffold' | tail -1) HEAD -- src | head -1)\" ]"
chk "feature visible in code (bulk-status or chosen feature in PROMPT)" "grep -rqiE 'bulk-status|BulkStatus' src/main"
echo "== [D] 6 documents =="
chk "DEPLOYMENT.md present, no template placeholders" "[ -s DEPLOYMENT.md ] && ! grep -q '<your real\\|<paste\\|<path/to\\|<git sha' DEPLOYMENT.md"
chk "DEPLOYMENT.md shows new endpoint evidence"  "has DEPLOYMENT.md 'bulk-status|screenshot|https?://'"
chk "REFLECTION.md present, no placeholders"    "[ -s REFLECTION.md ] && ! grep -q '<2-3\\|<one SPECIFIC\\|<exact change' REFLECTION.md"
if [ -s REFLECTION.md ]; then w=$(wc -w < REFLECTION.md); chk "REFLECTION.md <= ~1 page (<=550 words; found $w)" "[ $w -le 550 ]"; fi
chk "DESIGN_BRIEF has sections A-D"             "for s in 'A. Intent' 'B. Governance' 'C. Non-Determinism' 'D. Architectural'; do has DESIGN_BRIEF.md \"\$s\" || exit 1; done"
chk "DESIGN_BRIEF draft banner removed"         "! has DESIGN_BRIEF.md 'DRAFT STATUS'"
chk "DESIGN_BRIEF example contract replaced"    "! has DESIGN_BRIEF.md 'replace with the real'"
bw=$(wc -w < DESIGN_BRIEF.md 2>/dev/null || echo 0)
if [ "$bw" -lt 1200 ] || [ "$bw" -gt 3200 ]; then wn "DESIGN_BRIEF word count $bw outside ~1200-3200 (3-5 pages)"; else ok "DESIGN_BRIEF length ~$bw words (3-5 pages)"; fi
chk "JOURNAL.md has no '(fill in)' left (bonus)" "[ -s JOURNAL.md ] && ! has JOURNAL.md 'fill in'"
chk "DESIGN_BRIEF references REFLECTION decision (D1/D2/D3)" "has REFLECTION.md 'D[123]'"
echo
echo "RESULT: $pass passed, $fail failed, $warn warnings"
[ "$fail" -eq 0 ]
