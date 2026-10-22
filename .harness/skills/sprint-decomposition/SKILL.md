# Sprint decomposition

Turn the exact developer prompt into a short, testable spec and ordered sprint
contracts. Do not implement code. Preserve prompt scope; list assumptions,
risks, exclusions, affected modules, endpoints, errors, events, and data
changes. When a contract choice changes API behavior, make that choice
explicit instead of leaving it to the Generator.

## Required spec format

The first line is exactly `STATUS: AWAITING APPROVAL`. Include `FEATURE_PROMPT`,
Goal, Out of scope, Affected modules, API contract, Data/audit contract,
Security, Sprint list, Risks, and unresolved decisions. Do not label a
specification APPROVED on the developer's behalf.

## Sprint contract format

Each contract includes:

```text
SPRINT: <number>
SPEC: .harness/output/spec.md
GATES_IN_SCOPE: <space-separated IDs>
FILES_EXPECTED: <paths, one per line>
```

List acceptance criteria under `ACs`. Give every criterion a stable
`AC-<sprint>.<number>` ID and exactly observable GIVEN/WHEN/THEN behavior.
Include success, business-rule, validation, authorization, and failure
outcomes. Every AC must be testable by an assertion of resulting state,
response data, event, or audit record—not solely HTTP status.

## Split rules

Split only when the risk or contract boundary justifies a separate sprint.
For the bulk-status demonstration, Sprint 1 owns service rules, item results,
scoped repository changes, audit records, and unit tests. Sprint 2 owns the
HTTP DTO/controller, response mapping, authentication integration tests, and
API documentation. Sprint 2 depends on Sprint 1. Do not split one logical
acceptance criterion across sprints.

## Batch semantics to settle in the spec

Specify supported status values, invalid top-level request behavior,
per-item not-found/foreign-store behavior, duplicate IDs, request ordering,
whether processing continues after item failure, result schema, and what
counts as the audit record. Prefer a durable in-process repository record over
calling an event notification an audit. No implicit whole-batch rollback.
