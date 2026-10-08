#!/usr/bin/env bash
# Deterministic verdict calculator. Same checks.env -> same verdict, always.
#
# Input : .harness/eval/checks.csv      (check registry: id,dimension,hard,source,description)
#         .harness/eval/weights.env     (dimension weights + thresholds)
#         .harness/output/checks.env    (A1=PASS ... plus LLM checks: A5=PASS and A5_EVIDENCE=path:line)
# Output: stdout lines VERDICT / SCORE / HARD_GATE_FAILURES / per-dimension scores / per-check table
#
# Rules (see .harness/skills/grading-criteria/SKILL.md):
#   R1 a check passes only if its value is exactly PASS (missing/ambiguous => FAIL)
#   R2 an LLM check passes only if it is PASS *and* has a non-empty <ID>_EVIDENCE=file:line entry
#   R3 any failed hard gate => FAIL regardless of score
#   R4 else score = sum(weight * passed/total) ; >=85 PASS ; 70-84 CONDITIONAL PASS ; <70 FAIL
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CSV="$ROOT/.harness/eval/checks.csv"
ENVF="${1:-$ROOT/.harness/output/checks.env}"
# shellcheck disable=SC1091
source "$ROOT/.harness/eval/weights.env"

declare -A VAL
while IFS='=' read -r k v; do
  [[ -z "${k// }" || "$k" =~ ^# ]] && continue
  VAL["$k"]="$(echo "$v" | tr -d '\r' | sed 's/^ *//;s/ *$//')"
done < "$ENVF"

declare -A TOTAL PASSED
hard_fail=()
table=""
while IFS=',' read -r id dim hard source desc; do
  [[ "$id" == "id" || -z "$id" ]] && continue
  TOTAL[$dim]=$(( ${TOTAL[$dim]:-0} + 1 ))
  ok=0
  if [[ "${VAL[$id]:-}" == "PASS" ]]; then
    ok=1
    if [[ "$source" == "LLM" && -z "${VAL[${id}_EVIDENCE]:-}" ]]; then ok=0; fi
  fi
  if (( ok )); then PASSED[$dim]=$(( ${PASSED[$dim]:-0} + 1 )); res=PASS; else res=FAIL; fi
  if [[ "$hard" == "Y" && $ok -eq 0 ]]; then hard_fail+=("$id"); fi
  table+="$id | $dim | hard=$hard | $source | $res | ${VAL[${id}_EVIDENCE]:--}"$'\n'
done < "$CSV"

score=$(awk -v t1="${TOTAL[D1]:-0}" -v p1="${PASSED[D1]:-0}" -v w1="$D1_WEIGHT" \
            -v t2="${TOTAL[D2]:-0}" -v p2="${PASSED[D2]:-0}" -v w2="$D2_WEIGHT" \
  'BEGIN{ s=0; if(t1>0) s+=w1*p1/t1; if(t2>0) s+=w2*p2/t2; printf "%d", s }')
d1=$(awk -v t="${TOTAL[D1]:-0}" -v p="${PASSED[D1]:-0}" -v w="$D1_WEIGHT" 'BEGIN{printf "%.1f", (t>0)?w*p/t:0}')
d2=$(awk -v t="${TOTAL[D2]:-0}" -v p="${PASSED[D2]:-0}" -v w="$D2_WEIGHT" 'BEGIN{printf "%.1f", (t>0)?w*p/t:0}')

if (( ${#hard_fail[@]} > 0 )); then verdict="FAIL"
elif (( score >= PASS_THRESHOLD )); then verdict="PASS"
elif (( score >= CONDITIONAL_THRESHOLD )); then verdict="CONDITIONAL PASS"
else verdict="FAIL"; fi

echo "VERDICT: $verdict"
echo "SCORE: $score"
echo "HARD_GATE_FAILURES: ${hard_fail[*]:-none}"
echo "D1 ($D1_NAME, ${D1_WEIGHT}%): $d1 / $D1_WEIGHT"
echo "D2 ($D2_NAME, ${D2_WEIGHT}%): $d2 / $D2_WEIGHT"
echo "---"
printf "%s" "$table"
