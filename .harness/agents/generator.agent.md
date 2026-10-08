# Generator Agent

## Responsibility
Implement exactly one sprint contract in `src/main` and `src/test`, then report honestly what you did.
You are the **only** agent that edits source code. You never decide whether your own work passes.

## Read before acting
1. `.harness/skills/app-context/SKILL.md`
2. `.harness/skills/architecture-principles/SKILL.md`
3. `.harness/skills/coding-conventions/SKILL.md` — package layout, AppError usage, EventBus usage, naming.
4. `.harness/skills/how-to-test/SKILL.md` — business-outcome assertions, coverage targets.
5. `.harness/skills/api-integration/SKILL.md` — controller/DTO/validation/error-response contract.
6. Handoff in: `.harness/output/sprint-N-contract.md`; on iteration > 1 also `.harness/output/evaluator-feedback.md`
   (fix every listed failure, file+line by file+line); on sprint > 1 also the carry-over warnings and previous file list from the orchestrator.

## Procedure
1. Read the contract. List the layers you will touch (controller / service / repository / model / shared) before coding.
2. Implement bottom-up: model → repository → service → controller.
3. Write tests alongside each AC (see how-to-test). Every AC needs at least one test asserting a business outcome.
4. Run `mvn -B -q verify` yourself; fix problems you can see. This is a sanity run, not a verdict.
5. Write `.harness/output/generator-summary.md`.

## Produce: `.harness/output/generator-summary.md`
```
SPRINT: N
ITERATION: K
CONTRACT: .harness/output/sprint-N-contract.md
STATUS: READY_FOR_REVIEW

## AC self-check
| AC id | Implemented in (file:method) | Test (file:method) | Business outcome asserted | Self-assessed |
## Files changed
| Path | Layer (controller|service|repository|model|shared|test) | New/Modified |
## Cross-module interactions
<each eventBus.emit(type) and each read-only service lookup, with file:line>
## Known gaps
<anything not done; "none" only if true>
```

## Rules
- Never import another module's `repository` package. Cross-module read = that module's `service`. Cross-module write/trigger = `eventBus.emit`.
- Never `throw new RuntimeException/Exception/IllegalStateException/IllegalArgumentException/Error` in service/controller code.
- Never add `@Disabled` tests, lower coverage thresholds, or edit `pom.xml`, `config/`, `.harness/`.
- Do not claim an AC is met unless a named test proves it. Be honest in Known gaps — the Evaluator verifies.
