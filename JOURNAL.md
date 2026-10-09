# Architecture Journal

- **Spec inconsistency:** the PDF's verify step calls `GET /api/tasks`, but the base API surface is `/api/activities`. Verification uses `/api/activities`.
- **ArchUnit instead of depcruiser:** depcruiser is a JS tool; ArchUnit is the Java equivalent and runs inside `mvn test` (see DESIGN_BRIEF D2).
- **Verdict by script:** early idea was to let the Evaluator output a score; moved arithmetic to `compute-verdict.sh` after realising identical check results must yield identical verdicts.
- **Identity via `X-Staff-Id`:** real auth is out of scope; the `staff` module is read-only lookup, which also demonstrates the "users read-only for other modules" rule.
- **Not yet verified in the authoring sandbox:** Maven build (no Maven/network available when the scaffold was written). First action on a real machine: `mvn -B verify` and record every correction here — those corrections become skill rules / gates.
- **Bug found in review (pom.xml):** `failOnViolation` was hard-coded to `true`, so `collect-gates.py`'s `-Dcheckstyle.failOnViolation=false` was silently ignored; one Checkstyle error would stop the build at `validate` and no ArchUnit/JaCoCo output would exist (every gate FAIL with no evidence). Fixed by moving it to the `checkstyle.failOnViolation` property that the CLI can override.
- **`tests/` vs Maven layout:** the spec lists `tests/` mirroring `src/`; Maven's `src/test/java/com/storeops/<module>/` is the Java equivalent and mirrors the module structure.
- **Review rule:** gate-collector and verdict scripts were tested with fixtures (determinism, hard-gate override, missing-evidence rejection). `verify-submission.sh` re-runs those fixtures and checks every PDF requirement; run it before submitting.
- **Observations during the demo run:** _(fill in)_
