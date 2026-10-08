# Skill: sprint decomposition (Planner)

**Purpose:** how to split a StoreOps feature into sprints and write testable acceptance criteria.

## Sprint boundary rules
1. One sprint = one **vertical, independently verifiable slice** that leaves `mvn -B verify` green.
2. Order: **Sprint 1 = domain + service + repository + events** (no HTTP); **Sprint 2 = controller + request/response contract + API tests**.
   Rationale: the architectural risks (FM1, FM2, FM4) live in service/repository code, so they are gated before any HTTP code exists.
3. If a feature adds a new event type or cross-module consumer, that is its own sprint item with its own AC naming the event and payload.
4. At most **6 ACs per sprint**; if more, split. Max 3 sprints per run.

## AC format (mandatory)
```
AC-<sprint>.<n>: <short title>
GIVEN <concrete initial state with ids/roles/statuses>
WHEN  <one action: service call or HTTP request with exact path/body>
THEN  <observable outcome 1: state or response body field>
AND   <observable outcome 2: event emitted / audit entry / error type + status>
```
An AC is **testable** only if every THEN names something an assertion can read: a field value, a list size, an `AppError` subclass + code,
an `eventBus.emit` type and payload key. Words like "works", "handles", "appropriately" are forbidden.
Every sprint must include at least one **error-path AC** (names the `AppError` subclass) and at least one **business-rule AC** (not just 2xx).

## Contract file format — `.harness/output/sprint-N-contract.md`
```
SPRINT: N
SPEC: .harness/output/spec.md
FEATURE_PROMPT: PROMPT.md
GOAL: <one sentence>
LAYERS_IN_SCOPE: <model, repository, service, controller>
FILES_EXPECTED: <package paths>
FAILURE_MODES_EXPOSED: <FM1..FM4 relevant>
GATES_IN_SCOPE: <A1..A8, T1..T6>
## Acceptance criteria
AC-N.1 ...
## Out of scope
```

## spec.md sections
`STATUS: AWAITING APPROVAL` (line 1) · `FEATURE_PROMPT: PROMPT.md` · Goal · Out of scope · Affected modules · Endpoints · New event types
(name + payload keys) · Sprint list with one-line rationale per boundary · Risks.
