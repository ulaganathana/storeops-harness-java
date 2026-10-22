ARCHIVE STATUS: APPROVED
FEATURE STATUS: APPROVED

FEATURE_PROMPT: PROMPT.md

## Goal
Allow an authenticated staff member to submit one ordered batch of activity
status changes to DONE or BLOCKED. Process each activity independently,
continue after item failures, return one outcome per ID, and record exactly one
audit entry for every successful status change.

## Out of scope
Persistent database storage, bulk changes to any fields other than status,
changes to programme membership or alerts, atomic whole-batch rollback, and
new authentication roles.

## Affected modules
`activities` routes/model/service/repository and `alerts` event subscription.
`common` errors/security/events remain shared interfaces. No direct
dependency on another feature module's implementation packages. Audit records
are owned by `activities`.

## API contract
`PATCH /api/activities/bulk-status`, authenticated by the existing
`X-User-Id` header. Body: `{"activityIds":["act-1","act-2"],"status":"DONE"}`.
The collection must be non-empty; IDs must be nonblank; target status is
exactly `DONE` or `BLOCKED`. Return a result in input order for every item.
Successful items carry `SUCCEEDED`; unknown or cross-store items carry a
per-item `NOT_FOUND` result and do not prevent subsequent processing.
An already-DONE activity cannot be changed to BLOCKED and returns
`CONFLICT`; other eligible items continue processing. Duplicates are
processed once: first occurrence performs the change and later occurrences
return `CONFLICT` without another audit write. HTTP status is 207 for mixed
outcomes, 200 for all success, and 404 for all item failures.
Malformed or invalid top-level requests use the existing 400 error envelope.

## Data and audit contract
Add `BLOCKED` to activity status. Each success persists the new status and
creates one in-memory activity audit entry containing activity ID, actor ID,
previous status, new status, and timestamp. Failed and duplicate items create
no audit entry. Each successful change also publishes one
`ACTIVITY_STATUS_CHANGED` event. Audit history is process-local, matching the
in-memory repository's lifetime; it is not durable across restarts.

## Security
Resolve the caller using the existing interceptor. Only update records in
the caller's store. Do not reveal the existence of records in other stores.
The actor in each audit entry is derived from `CurrentUser`, never the body.

## Sprint list
1. Service, repository, audit model/storage, item-result contract, and
   business-rule tests.
2. HTTP request/response integration, endpoint documentation, and route tests.

## Risks
The feature prompt does not define duplicate handling or status codes for
partial batches; the rules above follow the worked Section 3.4 example where
a DONE activity requested as BLOCKED fails with CONFLICT. Confirm these
proposed details at the approval gate. The process-local audit repository is
not a compliance-grade durable audit system.
