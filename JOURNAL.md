# Architecture Journal

- **Spec inconsistency:** the PDF's verify step calls `GET /api/tasks`, but the base API surface is `/api/activities`. Verification uses `/api/activities`.
- **ArchUnit instead of depcruiser:** depcruiser is a JS tool; ArchUnit is the Java equivalent and runs inside `mvn test` (see DESIGN_BRIEF D2).
- **Verdict by script:** early idea was to let the Evaluator output a score; moved arithmetic to `compute-verdict.sh` after realising identical check results must yield identical verdicts.
- **Identity via `X-Staff-Id`:** real auth is out of scope; the `staff` module is read-only lookup, which also demonstrates the "users read-only for other modules" rule.
- **Not yet verified in the authoring sandbox:** Maven build (no Maven/network available when the scaffold was written). First action on a real machine: `mvn -B verify` and record every correction here — those corrections become skill rules / gates.
- **Observations during the demo run:** _(fill in)_
