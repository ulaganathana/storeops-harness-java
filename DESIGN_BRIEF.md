# Harness Design Brief — StoreOps Claude Code Harness (Java)

> **DRAFT STATUS — read before submitting.** Sections B, C, D describe the harness as built and are ready to adapt. Section A contains a *worked example
> contract written before the run*: after your demonstration run, replace it with the real `sprint-1-contract.md` entry and correct any sprint boundaries
> that changed. The rationale and assumptions must be yours — reviewers will probe them.

## A. Intent Decomposition

**Feature (PROMPT.md):** shift handover bulk update — `PATCH /api/activities/bulk-status`, partial failures, one audit entry per updated activity.

**Agents.** The Planner owns *what* (spec + contracts), the Generator owns *how* (only agent that edits `src/`), the Evaluator owns *whether* (never edits code),
the Monitor owns *what happened* (never judges). Each responsibility was derived from a separate failure of the client's experiment: unreviewed generation,
self-graded generation, and no record of why code was accepted.

**Sprint boundaries.** Sprint 1 = model + service + repository + events (no HTTP). Sprint 2 = controller + 207 response contract + API tests.
The boundary is drawn where the risk changes: FM1/FM2/FM4 (boundary, errors, events) live in service code, so they are gated while the diff is small and
before HTTP concerns hide them. Sprint 2 can only fail on contract/test issues, which makes escalations easy to diagnose.

**GIVEN/WHEN/THEN testability.** Every THEN must name a readable artefact — a field, a list size, an `AppError` subclass+code, an `eventBus.emit` type and payload key.
Subjective words ("handles", "works") are banned by the sprint-decomposition skill. Each sprint needs ≥1 business-rule AC and ≥1 error-path AC so FM3 cannot hide.

**Example contract entry (replace with the real one after the run):**
```
SPRINT: 1   SPEC: .harness/output/spec.md   GATES_IN_SCOPE: A1 A2 A3 A6 A7 T3 T4
AC-1.2: Partial failure does not abort the batch
GIVEN activities a1 (TODO), a2 (DONE), a3 (IN_PROGRESS) exist and caller is s-3
WHEN  ActivityService.bulkUpdateStatus("s-3", [a1,a2,a3], BLOCKED) is called
THEN  the result lists a1=SUCCEEDED, a3=SUCCEEDED, a2=FAILED with error code CONFLICT
AND   a1 and a3 persist status BLOCKED, a2 remains DONE
AND   eventBus emitted ACTIVITY_STATUS_CHANGED exactly twice (a1, a3), none for a2
```

## B. Governance Framework

**Skill-file strategy (8 files; every file earns its place).**

| Skill | Read by | Encodes (StoreOps-specific) |
|---|---|---|
| app-context | all four agents | modules, 9 endpoints, seed staff ids, enums, existing events, `mvn -B verify`. Deliberately ≈1 page: it is paid for by every agent every iteration |
| architecture-principles | Planner, Generator, Evaluator | rules R1–R6 each tied to a failure mode and a gate id |
| sprint-decomposition | Planner | sprint order rule, AC grammar, contract header markers |
| coding-conventions | Generator | package layout, `AppError` subclass→status map, `EventBus.emit` pattern, null-safe payloads |
| how-to-test | Generator | JaCoCo 80/70/60/70, `RecordingEventBus`, "status-only is not an AC" |
| api-integration | Generator | 207 partial-failure envelope, header auth, `ErrorResponse` shape |
| how-to-review | Evaluator | yes/no question + evidence format for each LLM check |
| grading-criteria | Evaluator | dimensions, weights, hard gates, verdict arithmetic |

Shared-by-all files are the facts and rules that must never diverge between agents; role-specific depth is isolated so only one agent pays its token cost.
Trade-off acknowledged: deeper skills reduce drift but increase per-invocation cost; depth was concentrated in the two agent-specific skills (`coding-conventions`, `how-to-review`).

**`.harness/reviews/` as audit trail.** Per sprint it holds the generator summary, the final and every failed-iteration evaluator feedback, and the Monitor's `run-log`
(sprint id, verdict, score, iterations, escalation flag, gates failed per iteration, token estimate, trend notes). It is committed to Git, so the standards team and
reviewers read it through normal repository access and `git log` gives time order. A recurring problem surfaces when the same gate id appears in
`HARD_GATES_FAILED_BY_ITERATION` across sprints; the Monitor then names the skill file that should absorb the rule ("skill-file drift signals").

**One rule and what breaks without it.** *R3: `eventBus.emit(EventTypes.SLA_BREACH, payload)` is the only permitted cross-module trigger; importing `AlertService`
(or any `reports` class) from another module is a hard-gate failure (ArchUnit `A3`).* Without it the Generator takes the shortest path — calling the notification
service directly — which recreates FM4: `activities` becomes coupled to `alerts`, delivery rules cannot change without touching activities, and circular
imports appear as soon as `alerts` reads an activity.

**CI/CD relationship.** The harness *precedes and feeds* CI: the Evaluator runs the same `mvn -B verify` as the pipeline, so violations are caught before a PR, and CI re-runs it
as the independent second line. The harness adds LLM-assessed checks CI cannot express. Harness files live in `.harness/`, pipeline files in `.github/`.

## C. Non-Determinism Strategy

**Dimensions.** D1 Architectural Compliance **55%**, D2 Test & Build Quality **45%**. Three of the four client failure modes are architectural, so D1 weighs more;
D2 carries FM3 and build health.

**Hard gates (each prevents one named failure; none can be soft).**

| Gate | Tool | Prevents | Why not soft |
|---|---|---|---|
| A1 module boundary | ArchUnit | FM1 repository bypass | a single bypass breaks the service boundary; scoring it invites trading it off against coverage |
| A2 raw throws | Checkstyle `RawThrow` + ArchUnit | FM2 | raw exceptions skip `GlobalExceptionHandler` → 500s, lost codes |
| A3 event bus only | ArchUnit | FM4 | direct calls re-couple modules |
| A4 layering | ArchUnit | logic in wrong layer | not fixable by review comments later |
| A5 reports read-only | LLM + evidence | write from reports | integrity |
| T1 build green / T2 coverage | Maven / JaCoCo | broken or under-tested code | contract with the standards team |
| T3 AC→business assertion | LLM + evidence | **FM3** | only LLM-assessable rule, so made strict: evidence required, ambiguity = FAIL |

**Variable → deterministic.** (1) Everything measurable is measured by a tool and written to `checks.env` by a script. (2) LLM judgement is confined to seven yes/no questions;
`PASS` is accepted only with `<ID>_EVIDENCE=file:line`, otherwise the script records FAIL. (3) The Evaluator never computes the verdict: `compute-verdict.sh` does, from `checks.env`.
(4) Malformed Evaluator output is treated as FAIL by the orchestrator.

**Worked example.** Generator output *X*: all tools green; A6–A8, T4–T6 PASS; T3 → Evaluator finds AC-1.3 asserted only `status().isOk()` and writes `T3=FAIL`.
Script: hard gate T3 failed ⇒ `VERDICT: FAIL` even though the score is 92. Re-running `compute-verdict.sh` on the same `checks.env` yields byte-identical output (checked with md5 on repeated runs; keep a sample under `.harness/reviews/` as evidence). Output *Y*: two soft checks fail (A6, A7), no hard gate fails ⇒ score 86 ⇒ `PASS`
(`CONDITIONAL PASS` would need 70–84).

**Escalation.** Triggered by the 3rd consecutive `FAIL` in a sprint. The orchestrator writes `.harness/output/escalation.md` (sprint, iterations, last score, blocking gates, recurring failures,
suspected root cause class, one recommended human action, resume commands). It goes to the developer who started the run; the Monitor also logs `ESCALATION: TRUE` in
`.harness/reviews/` for the standards team. Expected response: fix the contract/skill/tooling, then `CONTINUE` or `REPLAN`.

## D. Architectural Decisions

**D1. A script owns the verdict, not the Evaluator LLM.**
Alternatives: (a) LLM computes score and verdict; (b) LLM assesses everything with a rubric; (c) script computes from check results (chosen).
Rationale: identical check results must give identical verdicts; arithmetic is the first thing LLM leniency erodes. *Assumption:* check ids and the evidence requirement are enforced
by the script, and the Evaluator cannot edit `.harness/scripts` (denied in `.claude/settings.json`).

**D2. ArchUnit for FM1/FM3-adjacent boundaries instead of Checkstyle ImportControl or an LLM check.**
Alternatives: Checkstyle `ImportControl` (import-text only, misses fully-qualified usages and transitive structure), LLM review (non-deterministic).
Rationale: ArchUnit analyses bytecode, handles slices/cycles, and runs in `mvn test` so CI gets it free. *Assumption:* package-per-module naming (`com.storeops.<module>.<layer>`) is kept;
a renamed package silently weakens the rules, which is why each rule is parameterised by module name and the Monitor watches for gate drift.

**D3. Two-sprint split (service-first, HTTP-second) with a hard cap of 3 iterations.**
Alternatives: one sprint per feature; one sprint per layer-file. Rationale: single sprint hides root causes and inflates diffs; per-file sprints multiply Evaluator cost. Three iterations
balances recovery against runaway cost. *Assumption:* a feature of this size yields ≤6 ACs per sprint and the Generator converges within three feedback rounds; if the Monitor shows
average iterations ≥2.5 over several sprints, the cause is skill-file gaps, not the cap.
