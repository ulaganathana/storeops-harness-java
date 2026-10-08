# Skill: grading criteria (Evaluator)

**Purpose:** the fixed evaluation framework. The registry of checks is `.harness/eval/checks.csv`; weights are in `.harness/eval/weights.env`.
Do not change either.

## Dimensions
| Dimension | Weight | Why for StoreOps |
|---|---|---|
| **D1 Architectural Compliance** | **55%** | Three of the four client failure modes (FM1, FM2, FM4) are architectural. Highest weight. |
| **D2 Test & Build Quality** | **45%** | FM3 (status-only tests) plus build/coverage/lint health. |
Weights sum to 100%.

## Hard gates (any one failed ⇒ `FAIL`, regardless of score)
| Gate | Check | Tool | Prevents | Why it cannot be soft |
|---|---|---|---|---|
| A1 | Zero cross-module repository imports, no cycles | ArchUnit | FM1 | One violation breaks the service boundary; partial credit would normalise it |
| A2 | Zero raw throws in service/controller | Checkstyle `RawThrow` + ArchUnit | FM2 | Raw errors bypass `GlobalExceptionHandler` → 500s and lost error codes |
| A3 | No cross-module dependency on alerts/reports packages | ArchUnit | FM4 | Direct calls re-couple modules |
| A4 | Controller→service→repository only | ArchUnit | layering | Business logic in the wrong layer is not recoverable by review |
| A5 | Reports module never writes | LLM, evidence required | read-only rule | Data integrity |
| T1 | `mvn -B verify` exit 0, zero Checkstyle violations | Maven | broken build | Nothing downstream is meaningful on a red build |
| T2 | JaCoCo 80/70/60/70 | JaCoCo `check` | under-tested code | Threshold is a contract with the standards team |
| T3 | Every AC has a business-outcome assertion | LLM, evidence required | **FM3** | A status-only suite gives false confidence |
Soft checks: A6, A7, A8, T4, T5, T6 (counted in the score).

## Verdict rules (implemented by `compute-verdict.sh`)
1. A check passes only if its value is exactly `PASS`; LLM checks additionally need `<ID>_EVIDENCE=file:line`. Missing/other ⇒ FAIL.
2. Any failed hard gate ⇒ `VERDICT: FAIL`.
3. Else `score = Σ weight_d × passed_d / total_d` (all checks in a dimension weigh equally).
4. `score ≥ 85` ⇒ `PASS`; `70 ≤ score < 85` ⇒ `CONDITIONAL PASS`; `< 70` ⇒ `FAIL`.
Same `checks.env` ⇒ same verdict, always.

## Handling LLM variability
LLM judgement is confined to 7 narrowly phrased yes/no checks with mandatory evidence; everything that can be measured by a tool is measured by a tool.
Ambiguity ⇒ FAIL. The Evaluator never computes the verdict.
