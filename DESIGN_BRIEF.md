# StoreOps Claude Code Harness — Design Brief

## A. Intent decomposition

**Feature under test.** The run uses the committed feature prompt: shift
handover bulk update through `PATCH /api/activities/bulk-status`. Outgoing
shift staff can request DONE or BLOCKED for multiple activities. Each ID
produces an independent result, a failing item does not abort later items,
and each successful update produces one audit entry.

**Why this feature is a useful harness test.** The feature crosses request
validation, store-scoped authorization, status mutation, batch result
semantics, and audit integrity. A happy-path-only test could return HTTP 200
while updating the wrong store, lose one item on error, or create duplicate
audit entries. The contract therefore requires assertions over state and
audit records for every branch, not merely response status.

**Roles and separation.** Planner owns the specification and sprint
contracts; Generator owns implementation; Evaluator owns independent
assessment; Monitor owns the audit narrative. The orchestrator controls
approval, routing, evidence capture, and iteration limits, but does not
rewrite agent findings. Separating these roles prevents the implementer from
grading its own work and prevents an evaluator from silently repairing the
code it is meant to judge.

**Sprint boundary.** Sprint 1 covers domain/service behavior, repositories,
audit representation, item-level failures, and focused tests. Sprint 2 adds
the HTTP request/response contract, controller integration, endpoint docs,
and route tests. The split isolates business-rule failures before HTTP
mapping, while Sprint 2 can validate the final public API against already
tested service semantics. Sprint 2 depends on Sprint 1.

**Acceptance criteria.** Each criterion uses an AC identifier and observable
GIVEN/WHEN/THEN statements. A valid result must name changed status, per-item
result, audit entry count/contents, or absence of writes on failure. The
Planner must settle duplicate handling, batch status codes, ordering, and
audit lifetime in the proposed contract and pause for developer approval.
These choices are not present in the short feature prompt; they are
assumptions, not facts. The audit storage in this scaffold is process-local
and must not be represented as durable compliance storage.

**Example sprint contract entry (Sprint 1, AC-1.2):**

> GIVEN IDs [TODO, DONE, IN_PROGRESS] are requested for BLOCKED  
> WHEN the service processes the batch  
> THEN results preserve request order, the DONE item is CONFLICT, the other
> two activities become BLOCKED, exactly two audit records are written, and
> exactly two ACTIVITY_STATUS_CHANGED events are published.

## B. Governance framework

The skills are separated by stable shared context versus role-specific
procedure. `app-context` describes the actual Java 21/Spring Boot 3.3
application and current API. `architecture-principles` states module,
layering, error, and audit rules. `sprint-decomposition` defines testable
contracts. Generator-only skills cover coding, tests, and API integration;
Evaluator-only skills cover evidence review and grading. The additional
`how-to-review` and `grading-criteria` documents make judgment bounded and
repeatable rather than embedding review policy in the agent prompt.

This organization avoids role drift while limiting repeated context. Shared
facts are short and loaded by multiple agents; implementation and review
depth remains role-specific. Skills describe how to do the work, not
permission to bypass the approved contract.

**Review archive.** `.harness/reviews/` is committed with the implementation
and contains the exact prompt, spec, contracts, Generator summary, Evaluator
feedback, and Monitor log. It makes the path from request to evidence
inspectable through normal repository access and Git history without relying
on transient Claude Code chat history. The Monitor compares failed gate IDs
and iteration trends across sprints to identify rules that need stronger
skills or automation. `.harness/output/` is ignored scratch state and may be
regenerated; it is not the audit trail.

**Failure modes covered.** Module imports and layer boundaries are checked
automatically. Raw exceptions are prohibited in route/service code. Tests
must assert business outcomes, including successful audit writes and no
audit write on failures. Direct calls into peer modules are not an acceptable
substitute for an event bus, though the audit repository itself remains
owned by the activities module. Any non-automated judgment requires a cited
file/line; missing evidence is a failure, not a guess.

## C. Non-determinism strategy

Two weighted dimensions total exactly 100%.

| Dimension | Weight | Coverage |
|---|---:|---|
| D1 Architectural Compliance | 55% | module boundaries, raw throws, audit ownership, route/service separation |
| D2 Test and Build Quality | 45% | Maven test/verify, coverage, business assertions, test hygiene |

| Gate | Hard | Evidence and prevented failure |
|---|---|---|
| A1 | Yes | `ModuleBoundaryTest`/Maven verify: prevents peer repository imports and cycles (FM1). |
| A2 | Yes | `ModuleBoundaryTest` and Checkstyle: prevents raw route/service throws bypassing `AppError` (FM2). |
| A3 | Yes | Evaluator evidence review: prevents direct cross-module side effects bypassing `EventBus` (FM4). |
| T1 | Yes | `mvn test`: prevents shipping code that does not compile or pass tests. |
| T2 | Yes | JaCoCo in `mvn verify`: enforces the project's service/route/shared/overall coverage contract. |
| T3 | Yes | AC-to-business-assertion evidence review: prevents status-only tests (FM3). |
| T5 | Yes | `verify-disabled-tests.py`: prevents silently skipped tests from inflating confidence. |

Automated hard gates exist in both dimensions: A1/A2 for D1 and T1/T2/T5
for D2. Any failed hard gate forces FAIL regardless of weighted score.

Each gate result is binary PASS, FAIL, or BLOCKED and is stored with evidence.
The deterministic script validates the configured check IDs, enforces that
dimension weights sum to 100, calculates each dimension's fraction of passing
checks, and then applies the weights. PASS is 85 or above; CONDITIONAL PASS
is 70 through 84.99; below 70 is FAIL. Any failed or blocked hard gate is
FAIL. Stable input yields byte-stable score and verdict; the LLM does not do
the arithmetic or decide whether a hard gate can be waived.

Automated results are not inferred from previous `target/` output. Commands
must run against the current source; an unavailable Maven or Docker binary is
recorded as BLOCKED. LLM reviewers must return one PASS/FAIL result per
check and cite path:line evidence; ambiguous or missing output is BLOCKED.
The verdict script rejects missing IDs and passing results without evidence.
A summary or screenshot cannot replace a test log. CI re-runs automated
checks as a separate second line.

**Deterministic walkthrough.** The Generator may produce two otherwise
identical diffs. In one, the AC-2.1 test asserts only `status().isMultiStatus()`;
in the other it asserts result ordering, persisted status, and audit count.
T3 is FAIL for the first and PASS for the second, with test-line evidence.
Given the first result file, the script records a hard-gate failure and
returns FAIL even if all other weighted checks pass. Re-running the script on
the same CSV gives the identical score and verdict; prose cannot override it.

## D. Architectural decisions and trade-offs

**D1. Deterministic scoring script.** Alternatives were to let the Evaluator
calculate the score or grade the entire change qualitatively. The script was
selected because identical result files must produce identical verdicts.
The assumption is that the CSV check IDs and evidence fields remain the
stable interface; configuration changes must update the script in the same
reviewed change.

**D2. Service-first, two-sprint work.** Alternatives were one sprint mixing
all layers or one sprint per file. A single sprint would create a large
diff mixing item semantics, authorization, HTTP mapping, and test gaps.
Per-file sprints add handoff overhead without meaningful risk reduction.
Two sprints give a stable business layer to integrate and an explicit
dependency. The assumption is that the REST boundary can be tested against
the service contract without changing its business rules.

**D3. Three-iteration cap and approval gate.** Alternatives were unlimited
repair loops or developer review after every iteration. A cap of three
limits cost and creates a named human escalation point; exact `APPROVED`
after `AWAITING APPROVAL` keeps unresolved API assumptions out of generated
code. The assumption is that a human is available to resolve recurring
contract/tooling failures. On the third FAIL, the orchestrator records the
blocking gates and action in `escalation.md` and stops.
