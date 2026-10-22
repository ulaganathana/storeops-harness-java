ARCHIVE STATUS: APPROVED
SPRINT: 1
SPEC: .harness/reviews/spec.md
GATES_IN_SCOPE: A1 A2 A3 A4 T1 T3 T4 T5 T6
FILES_EXPECTED:
src/main/java/com/storeops/activities/model/ActivityStatus.java
src/main/java/com/storeops/activities/model/ActivityAudit.java
src/main/java/com/storeops/activities/repository/ActivityAuditRepository.java
src/main/java/com/storeops/activities/service/ActivityService.java
src/test/java/com/storeops/activities/ActivityServiceTest.java

## ACs

### AC-1.1: Update eligible activities and record audit
GIVEN an authenticated caller owns two activities in their store with statuses TODO and IN_PROGRESS
WHEN the service bulk-updates both to DONE
THEN both stored statuses are DONE and exactly one audit record per activity records the caller, prior status, new status, and timestamp.

### AC-1.2: Continue processing after a terminal-state conflict
GIVEN IDs [TODO, DONE, IN_PROGRESS] are requested for BLOCKED
WHEN the service processes the batch
THEN results preserve request order, the DONE item is CONFLICT, the other two activities become BLOCKED, exactly two audit records are written, and exactly two ACTIVITY_STATUS_CHANGED events are published.

### AC-1.3: Hide cross-store records
GIVEN an ID belongs to a different store
WHEN a caller from another store requests it in a batch
THEN that item returns NOT_FOUND, its status is unchanged, and no audit record is written for it.

### AC-1.4: Reject unsupported statuses without mutation
GIVEN a target status other than DONE or BLOCKED
WHEN the service validates the batch
THEN it raises `ValidationError` with code `VALIDATION_ERROR` before any activity or audit mutation.

### AC-1.5: Handle duplicate IDs once
GIVEN the same valid activity ID occurs twice in one request
WHEN the service processes the batch
THEN the first occurrence succeeds, the second returns CONFLICT, and exactly one status mutation and audit entry occur.

### AC-1.6: Continue processing after an unknown ID
GIVEN IDs [existing, missing, existing-2] are requested for DONE
WHEN the service processes the batch
THEN results preserve request order, the missing item is NOT_FOUND, both existing activities become DONE, and exactly two audit records and two status-change events are written.

## Failure modes
FM1 (cross-module repository coupling), FM2 (raw throws), and FM4 (direct
cross-module side effects) apply. FM3 applies to assertions that test only
status without state and audit outcomes.
