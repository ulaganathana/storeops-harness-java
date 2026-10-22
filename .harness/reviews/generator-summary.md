# Generator summary

**Run state: NOT RUN.** This is a factual implementation inventory, not an
output from the Generator agent. The spec is approved, but Claude Code is
unavailable; therefore no harness Generator invocation occurred.

## Implemented scope

- Activity vocabulary and staff roles were aligned to the Build Track; programme
  membership now uses Project, ProjectMember, and ProjectRole.
- Bulk activity status updates implement ordered per-item results, duplicate
  handling, conflict behavior, cross-store isolation, activity audit records,
  and `ACTIVITY_STATUS_CHANGED` events consumed by alerts.
- Added request validation, route integration, API documentation, and service
  and route coverage for outcomes, state, audit history, and access control.
- Added alert notification domain types and kept module communication on the
  shared event bus.
- Added deterministic gate collection, weighted verdict calculation, scoring
  regression tests, disabled-test checking, CI, Docker packaging, and submission
  documentation.

## AC self-check from repository evidence

| AC | Evidence | State |
|---|---|---|
| 1.1 | `ActivityServiceTest.bulkUpdateChangesMultipleEligibleActivitiesToDone` asserts both statuses, prior-state audit records, and two events. | Implemented; automated tests pass |
| 1.2 | `ActivityServiceTest` covers ordered processing across a DONE-to-BLOCKED conflict and verifies successful state/audit/event effects. | Implemented; automated tests pass |
| 1.3 | `ActivityServiceTest` and `ActivitiesRoutesTest` cover store isolation and NOT_FOUND without foreign mutation. | Implemented; automated tests pass |
| 1.4 | Service and route tests cover unsupported status validation and the 400 error envelope without mutation. | Implemented; automated tests pass |
| 1.5 | Service tests assert first duplicate succeeds, subsequent duplicate conflicts, and only one audit is written. | Implemented; automated tests pass |
| 1.6 | Route test covers mixed existing/missing/existing IDs, ordered outcomes, and per-ID audit counts. | Implemented; automated tests pass |
| 2.1 | Route test asserts HTTP 207, ordered mixed outcomes, persisted status, and exact audit counts. | Implemented; automated tests pass |
| 2.2 | Route tests cover HTTP 200 all-success and HTTP 404 all-failure responses. | Implemented; automated tests pass |
| 2.3 | Route tests cover empty, blank, malformed, and unsupported requests without mutation or audit writes. | Implemented; automated tests pass |
| 2.4 | Route test covers missing and unknown caller IDs; verifies unchanged activity state and no audit record. | Implemented; automated tests pass |

## Validation actually run

- `%TEMP%\\apache-maven-3.9.9\\bin\\mvn.cmd verify`: passed; 51 tests,
  zero failures/errors/skips; configured JaCoCo checks passed.
- `python .harness/scripts/verify-disabled-tests.py`: passed.
- `python .harness/scripts/test_compute_verdict.py`: 3 tests passed.
- Packaged application smoke request with `X-User-Id: u-1`: HTTP 200.
- Claude Code/Generator execution and Docker build/run: not performed.
