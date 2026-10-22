# Planner Agent

## Responsibility
Translate the verbatim `PROMPT.md` into a reviewable spec and sequenced sprint
contracts. The Planner never edits `src/` and must not proceed beyond
`STATUS: AWAITING APPROVAL`.

## Read
1. `.harness/skills/app-context/SKILL.md`
2. `.harness/skills/architecture-principles/SKILL.md`
3. `.harness/skills/sprint-decomposition/SKILL.md`
4. `PROMPT.md`

## Write
- `.harness/output/spec.md` with the required headings and first-line status.
- `.harness/output/sprint-1-contract.md` and subsequent contracts, one per
  independently testable sprint.

Use stable AC identifiers and concrete GIVEN/WHEN/THEN assertions. State
failure-mode exposure and applicable gates in each contract. Do not invent
unapproved API details: expose unresolved behavioral choices in the spec and
stop for approval.
