# StoreOps harness operating guide

Claude Code is the orchestrator, not a fifth implementation agent. Use the
four role files in `.harness/agents/` as the single source of agent
responsibilities. The only role permitted to edit `src/` is Generator.

## Entry format and prompt capture

Start a feature run with one message:

```text
@planner <feature request in one or two sentences>
```

Save the request verbatim as `PROMPT.md` before planning. Copy it to
`.harness/reviews/PROMPT.md` for the permanent run record. The reviewed
prompt is immutable during a run.

## Planner and approval gate

Invoke Planner with only the listed skills and prompt. Planner writes
`.harness/output/spec.md` with first line exactly `STATUS: AWAITING APPROVAL`
and one or more `sprint-N-contract.md` files. Show the complete spec and
contracts to the developer and stop. Do not invoke Generator, Evaluator, or
Monitor until the developer replies exactly `APPROVED`. Any other response
is revision feedback; ask Planner to update the same spec and contracts,
then request approval again. Do not treat the original feature request,
silence, or a tool result as approval.

On `APPROVED`, change the spec line to `STATUS: APPROVED`, save the approved
copy and contracts under `.harness/reviews/`, and proceed.

## Routing and maximum iterations

For each sprint, invoke Generator, then Evaluator, then route only from the
single machine-readable `VERDICT:` line and `HARD_GATE_FAILURES:` line in
evaluator feedback:

| Verdict | Action |
|---|---|
| `PASS` | Archive outputs; run Monitor; continue to next sprint. |
| `CONDITIONAL PASS` | Same; Monitor records soft failures as carry-over warnings. |
| `FAIL` | If fewer than 3 Generator iterations have run, archive feedback and rerun Generator with it. |
| Missing, malformed, or duplicate verdict | Treat as FAIL; record malformed output. |

There are at most **3 Generator iterations per sprint**. Never skip
Evaluator, change a gate or threshold, or infer a verdict from prose. After
iteration 3 fails, write `.harness/output/escalation.md` with sprint, gates,
iteration results, root-cause hypothesis, one human action, and CONTINUE or
REPLAN instructions. Stop implementation and notify the requesting developer.
Monitor still records the escalation.

## Context scoping and role routing

Start every agent invocation with a fresh, role-scoped context. Give each
agent only the skills named in its `.harness/agents/<role>.agent.md` file,
the approved spec, its active contract, and the explicit handoffs listed
there. Generator receives prior evaluator feedback and carry-over warnings;
Evaluator receives the diff and actual gate outputs; Monitor receives the
summaries and prior logs. Never pass unrelated chat history or entire
repository dumps when a bounded file list suffices.

The orchestrator tracks sprint, iteration, phase, and last verdict in
`.harness/output/state.md`. Working state stays in `.harness/output/`;
approved artifacts, feedback, summaries, and logs are copied to
`.harness/reviews/`. Keep `PROMPT.md` verbatim. Do not fabricate command
results, approvals, screenshots, or agent runs.

## Audit records

Every sprint archive contains the spec, its contract, generator summary,
final evaluator feedback, prior failed-iteration feedback if any, and Monitor
run log. Each document identifies its sprint and iteration. Review records
are committed with the source change so the acceptance rationale is
reviewable alongside the implementation.

## CI relationship

The harness is the pre-PR feedback loop, not a replacement for CI. Run the
same Maven test/verification commands locally that CI runs. Deterministic
architecture, compilation, test, lint, coverage, and disabled-test checks
are hard gates; CI reruns those checks independently on the PR. LLM checks
add traceability and contract review but cannot override a failed automated
gate. Harness materials live in `.harness/`; CI workflow configuration stays
in `.github/`.

## Escalation and integrity rules

Use `.harness/output/escalation.md` for a 3-failure stop, unavailable
required tooling, or an unresolved product decision that blocks implementation.
Do not bypass the approval gate to make a run appear complete. Do not edit
the approved contract, evaluator configuration, or evidence after evaluation
to manufacture a pass.
