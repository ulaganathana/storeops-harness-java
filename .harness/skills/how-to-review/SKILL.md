# Skill: how to review (Evaluator)

**Purpose:** a repeatable procedure that turns Generator output into binary check results. You are strict, literal and evidence-driven.

## Step 0 — Never trust the summary
Open the files. The summary tells you where to look, nothing more.

## Step 1 — Automated checks (do not interpret)
Run `python3 .harness/scripts/collect-gates.py`. Copy results as-is. `gate-details.txt` gives file:line for failures — reuse it in Findings.

## Step 2 — LLM checks: ask the exact yes/no question, cite evidence
Result is `PASS` **only** if the answer is an unambiguous YES and you can write `<ID>_EVIDENCE=<path>:<line>`. Otherwise `FAIL`. No "partially".

| ID | Question (answer YES/NO) | Where to look | Evidence to cite |
|---|---|---|---|
| A5 | Does any class in `com.storeops.reports` call `save/create/update/delete/add/emit-as-write` on another module's service, or hold a repository of another module? (YES = FAIL) If the module has no new code this sprint → PASS with evidence `reports/package-info.java:1` | `src/main/java/com/storeops/reports/**` | the line of the aggregating read call |
| A6 | For every cross-module side effect in the diff, is it done with `eventBus.emit(EventTypes.<X>, …)` and is `<X>` declared in `EventTypes`? (no direct call to another module's service to *cause* a change) | `git diff`, `EventTypes.java` | line of the `emit` call |
| A7 | Does each `throw` of an `AppError` use the subclass matching its meaning (missing→NotFound, bad input→Validation, role→Forbidden, state clash→Conflict)? | all `throw new` in the diff | line of the most significant throw |
| A8 | Does `generator-summary.md` "Files changed" list a layer for every file in `git diff --name-only`, and is each layer correct for the package? | summary vs diff | summary line |
| T3 | For **every** `AC-N.x` in the contract, does at least one test assert a state, event, audit entry or typed error (not only an HTTP status)? List each AC → test:line in **Traceability**; one AC with only status assertions = NO | `src/test/**` | test line of the weakest AC |
| T4 | For each new service method, is every `throw` branch exercised by a test? | service diff vs tests | line of a covering test |
| T6 | Does the summary AC table contain every AC id from the contract, each with a test reference? | summary vs contract | summary line |

## Step 3 — Compute, don't decide
Write all values to `.harness/output/checks.env`, run `.harness/scripts/compute-verdict.sh`, paste its output. The script owns the verdict.

## Step 4 — Write actionable findings
Format: `[A1] src/main/java/com/storeops/activities/service/ActivityService.java:42 — imports com.storeops.alerts.repository.AlertRepository — replace with eventBus.emit(EventTypes.SLA_BREACH, …)`.
Invalid feedback: "tests are insufficient", "does not follow conventions".

## Ambiguity fallback
If you cannot decide (file missing, unreadable, diff empty, summary contradicts code): result `FAIL`, finding text starts `AMBIGUOUS:` and states what is missing.
If you cannot produce a well-formed feedback file, write only `VERDICT: FAIL` and the reason; the orchestrator treats malformed output as FAIL.
