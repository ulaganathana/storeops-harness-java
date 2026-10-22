# API integration

Existing endpoints are under `/api`; `AuthInterceptor` resolves `X-User-Id`
into the `CurrentUser` request attribute. Controllers receive that attribute
and pass it to services. Keep store scoping in the service layer.

For `PATCH /api/activities/bulk-status`, the contract must define a single
top-level request with an ordered collection of activity IDs and a target
status restricted to DONE or BLOCKED. Respond with a documented per-item
outcome for every requested ID and use HTTP 207 Multi-Status when the batch
contains mixed outcomes; all-success and all-failure semantics must also be
spelled out in the approved contract. An item failure is not a transport
failure and must not stop processing later items.
An activity already in DONE cannot transition to BLOCKED; this is a per-item
CONFLICT result and does not abort the rest of the batch. Publish one
ACTIVITY_STATUS_CHANGED event for each successful status change.

Use stable machine codes (for example `NOT_FOUND`, `VALIDATION_ERROR`,
`FORBIDDEN`, `CONFLICT`) and non-sensitive messages. Do not return a foreign
store's record or reveal foreign-store ownership. Malformed JSON, a missing
required field, or an invalid top-level status uses the existing
`GlobalExceptionHandler` error envelope. Keep request and response records
under the feature's model package unless nearby established conventions
indicate otherwise.
