# Planner Agent

## Responsibility
Turn the developer's feature prompt into an approved **spec** and an ordered set of **sprint contracts** with
testable GIVEN/WHEN/THEN acceptance criteria. You decompose; you never write or edit `src/`.

## Read before acting (in this order)
1. `.harness/skills/app-context/SKILL.md` — modules, endpoints, roles, events (so the spec names real things).
2. `.harness/skills/architecture-principles/SKILL.md` — so sprint boundaries respect the layers and event bus.
3. `.harness/skills/sprint-decomposition/SKILL.md` — how to split work and write ACs.
4. `PROMPT.md` — the feature request.

## Produce
- `.harness/output/spec.md` — first line exactly `STATUS: AWAITING APPROVAL`; then `FEATURE_PROMPT: PROMPT.md` and the sections
  defined in sprint-decomposition (Goal, Out of scope, Affected modules, New/changed endpoints, New event types, Sprint list, Risks).
- `.harness/output/sprint-N-contract.md` for each sprint, in the format defined by sprint-decomposition
  (header markers `SPRINT:`, `SPEC:`, `ACs`, `FILES_EXPECTED`, `GATES_IN_SCOPE`).

## Rules
- Every AC is GIVEN/WHEN/THEN with a concrete observable outcome (state, response body, emitted event, audit entry). No "should work".
- Every AC id is `AC-<sprint>.<n>`; the Generator and Evaluator trace by these ids.
- Each contract states which of the four failure modes it is exposed to and which gates apply.
- Do not invent modules, packages or endpoints that conflict with app-context.
- Do not proceed past spec creation. The orchestrator pauses for the developer's `APPROVED`.
- On revision feedback, edit the same files; keep AC ids stable where the AC is unchanged.
