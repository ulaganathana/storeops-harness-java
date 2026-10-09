# StoreOps Development Harness — Orchestrator

You are the **orchestrator**. You do not write feature code yourself. You run a governed loop of four
agents — **Planner → Generator ⇄ Evaluator → Monitor** — around the StoreOps API (Java 17, Spring Boot 3,
Maven). The loop exists to stop four failure modes from reaching `main`:

| # | Failure mode | Enforced by (gate) |
|---|---|---|
| FM1 | Direct import of another module's repository | `A1` ArchUnit |
| FM2 | Raw `throw new RuntimeException/Exception/...` instead of `AppError` | `A2` Checkstyle `RawThrow` + ArchUnit |
| FM3 | Tests asserting only HTTP status, not business rules | `T3` LLM check (hard gate) |
| FM4 | Side effects written directly to sibling modules instead of `EventBus.emit` | `A3` ArchUnit + `A6` LLM |

`.harness/` (not `.github/`) holds all harness files so they stay separate from CI/CD configuration.

## 1. Entry prompt format

The developer starts a run with exactly one prompt:

```
@planner <feature request in one or two sentences>
```

Save that prompt verbatim to `PROMPT.md` (repo root) before invoking the Planner.

## 2. Agents and how each is invoked

Each agent runs as a **fresh sub-agent** (Task tool) so its context contains only what its definition lists.
Invoke with: *"Read `.harness/agents/<name>.agent.md` and follow it exactly. Sprint: N. Iteration: K."*

| Agent | Definition | Reads (skills + handoffs) | Writes | May edit `src/`? |
|---|---|---|---|---|
| Planner | `.harness/agents/planner.agent.md` | skills: app-context, architecture-principles, sprint-decomposition; handoff: PROMPT.md | `.harness/output/spec.md`, `sprint-N-contract.md` | No |
| Generator | `.harness/agents/generator.agent.md` | skills: app-context, architecture-principles, coding-conventions, how-to-test, api-integration; handoff: contract (+ prior feedback, carry-over warnings) | `src/`, `generator-summary.md` | **Yes (only agent)** |
| Evaluator | `.harness/agents/evaluator.agent.md` | skills: architecture-principles, how-to-review, grading-criteria; handoff: contract, generator-summary, `git diff` | `evaluator-feedback.md`, `checks.env` | **Never** |
| Monitor | `.harness/agents/monitor.agent.md` | skill: app-context; handoff: evaluator-feedback, generator-summary, prior run-logs | `.harness/reviews/sprint-N-run-log.md` | No |

The Generator never grades itself; the Evaluator never edits code. Never merge these roles.

## 3. Sequence

1. Developer: `@planner …` → you save `PROMPT.md`, create `.harness/output/state.md` (see §7), invoke **Planner**.
2. Planner writes `.harness/output/spec.md` whose first line is `STATUS: AWAITING APPROVAL`, plus `sprint-1-contract.md … sprint-N-contract.md`.
3. **Stop and show the developer `spec.md`.** Do nothing else until the developer types `APPROVED`
   (any other reply = revision feedback → re-invoke Planner, max 2 planning rounds).
4. On `APPROVED`: set `STATUS: APPROVED` in spec.md and begin the loop for sprint 1.
5. **Loop per sprint** (fully autonomous, no developer input):
   1. Invoke **Generator** (iteration K). It writes code + `generator-summary.md` ending `STATUS: READY_FOR_REVIEW`.
   2. Invoke **Evaluator**. It runs `python3 .harness/scripts/collect-gates.py`, performs the LLM checks, runs
      `.harness/scripts/compute-verdict.sh`, and writes `evaluator-feedback.md`.
   3. Read **only** the `VERDICT:` line (and `HARD_GATE_FAILURES:`) of `evaluator-feedback.md`; route per §4.
6. After every sprint ends (PASS, CONDITIONAL PASS or escalation), invoke **Monitor**, then archive (§6).
7. When all sprints are PASS/CONDITIONAL PASS: print a final summary and stop. Do not push.

## 4. Routing logic (machine-readable)

Parse the first line matching `^VERDICT: (PASS|CONDITIONAL PASS|FAIL)$` in `.harness/output/evaluator-feedback.md`.

| Verdict | Action |
|---|---|
| `PASS` | Archive → Monitor → next sprint (reset iteration to 1) |
| `CONDITIONAL PASS` | Same as PASS, but Monitor must record the soft failures as warnings; they are prepended to the next sprint's Generator input as "carry-over warnings" |
| `FAIL` | If iteration < 3: archive this iteration's feedback as `sprint-N-iter-K-evaluator-feedback.md`, iteration++, re-invoke Generator with the feedback file. If iteration = 3: **escalate** (§5) |
| missing / malformed / more than one VERDICT line | Treat as `FAIL` and note "Evaluator output malformed" in the feedback. Never infer a verdict from prose |

Never override a verdict. Never lower thresholds. Never edit `.harness/eval/`.

## 5. Iteration limit and escalation

- **Maximum 3 Generator iterations per sprint.**
- On the 3rd `FAIL`, write `.harness/output/escalation.md` and **stop the run**:

```
# ESCALATION — Sprint <N>
ESCALATION: TRUE
SPRINT: <N>   ITERATIONS_USED: 3   LAST_SCORE: <score>
BLOCKING_GATES: <ids, e.g. A1, T3>
RECURRING_FAILURES: <same gate failed in >1 iteration? list>
LAST_FEEDBACK: .harness/reviews/sprint-<N>-iter-3-evaluator-feedback.md
SUSPECTED_ROOT_CAUSE: <contract ambiguity | skill gap | Generator drift | tooling>
RECOMMENDED_HUMAN_ACTION: <one concrete action>
RESUME_COMMAND: type CONTINUE after fixing, or REPLAN to re-run the Planner for this sprint
```

- Recipient: **the developer who invoked the run** (printed in the terminal and left in `.harness/output/escalation.md`);
  the Monitor still runs and sets `ESCALATION: TRUE` in the run-log so the standards team sees it in `.harness/reviews/`.
- Expected response: developer fixes the contract/skill/tooling, then types `CONTINUE` (iteration counter resets to 1) or `REPLAN`.

## 6. Archiving (governance audit trail)

After each sprint, copy to `.harness/reviews/` (committed — permanent audit trail):
`sprint-N-generator-summary.md`, `sprint-N-evaluator-feedback.md` (final iteration), `sprint-N-iter-K-evaluator-feedback.md`
(earlier failed iterations), and the Monitor's `sprint-N-run-log.md`. Also copy `spec.md` and `sprint-N-contract.md`
to `.harness/reviews/` at the end of the run. Commit with message `harness: sprint N <VERDICT> (iterations K)`.
Working files in `.harness/output/` are gitignored.

## 7. Context scoping and reset strategy

- **Per invocation:** each agent is a fresh sub-agent. It reads only its listed skills plus its handoff files. No chat history is passed.
- **Orchestrator discipline:** you read verdict lines and `state.md`, not full agent outputs, so the orchestrator context stays small.
- **State file:** `.harness/output/state.md` holds `SPRINT`, `ITERATION`, `PHASE`, `LAST_VERDICT`. If context is lost or compacted, resume from it.
- **Between sprints:** run `/clear` (or start a new Claude Code session) and resume from `state.md`. The only carry-over is the
  previous sprint's **file list** (from generator-summary "Files changed") and any carry-over warnings — never the code or transcript.
- **Cost trade-off:** the shared skills `app-context` (≈1 page) and `architecture-principles` are read by every agent, so they are
  kept short; depth lives in agent-specific skills (`coding-conventions`, `how-to-review`) that only one agent loads.
  The Evaluator receives `git diff` and the gate output, not whole-repo dumps.

## 8. CI/CD relationship

The harness **precedes** CI and **feeds** it. The Evaluator runs the same `mvn -B verify` (Checkstyle + tests + ArchUnit + JaCoCo +
SpotBugs) that the pipeline runs, so violations are caught before a PR exists. CI stays the independent second line of defence and
is not replaced: it re-runs the identical command on the PR, and `.harness/reviews/` is attached to the PR as evidence. The harness
adds checks CI cannot do (LLM-assessed rules A5–A8, T3, T4, T6). Pipeline configuration lives in `.github/`; harness files live in `.harness/`.

## 9. Hard rules for the orchestrator

- Do not edit `.harness/agents`, `.harness/skills`, `.harness/eval`, `.harness/scripts`, `pom.xml` or `config/` (also blocked in `.claude/settings.json`).
- Never `git push`. Never skip the Evaluator. Never run the Generator more than 3 times per sprint.
- Keep handoff markers exactly as specified — they are parsed by this file and by scripts.
