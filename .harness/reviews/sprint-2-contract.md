ARCHIVE STATUS: APPROVED
SPRINT: 2
SPEC: .harness/reviews/spec.md
GATES_IN_SCOPE: A1 A2 A3 A4 T1 T2 T3 T4 T5 T6
FILES_EXPECTED:
src/main/java/com/storeops/activities/model/BulkStatusRequest.java
src/main/java/com/storeops/activities/model/BulkStatusResult.java
src/main/java/com/storeops/activities/routes/ActivitiesController.java
src/test/java/com/storeops/activities/ActivitiesRoutesTest.java
README.md

## ACs

### AC-2.1: Return ordered per-item outcomes
GIVEN a valid authenticated request containing one eligible ID and one missing ID
WHEN the caller invokes `PATCH /api/activities/bulk-status`
THEN HTTP 207 and ordered outcomes identify SUCCEEDED and NOT_FOUND, and persisted state plus audit history reflect only the successful item.

### AC-2.2: Return all-success and all-failure statuses
GIVEN an authenticated request with all IDs eligible, or all IDs missing
WHEN the caller submits the batch
THEN all-success returns HTTP 200 and all-failure returns HTTP 404, with a response item for each requested ID.

### AC-2.3: Validate top-level input
GIVEN a request with an empty ID list, blank IDs, malformed JSON, or an unsupported status
WHEN the caller submits it
THEN HTTP 400 returns the standard `code`/`message` error body and no activity or audit state changes.

### AC-2.4: Require known caller
GIVEN no `X-User-Id` header or an unknown user ID
WHEN the caller submits a batch
THEN HTTP 401 returns the standard error body, existing activity state is unchanged, and no audit record is written for the request.

## Documentation requirement
README documents method, path, body, supported statuses, item result shape,
HTTP outcomes, store isolation, duplicate behavior, and audit semantics.

## Failure modes
FM1, FM2, FM3, and FM4 apply. Regression risk includes existing activity
create/read/update/delete behavior and the shared error envelope.
