# StoreOps application context

StoreOps is a Java 21, Spring Boot 3.3.5 REST API with in-memory repositories.
The root package is `com.storeops`. Feature modules are `activities`,
`programmes`, `alerts`, `reports`, and `staff`; shared errors, events,
security, and utilities live in `common`.

## Runtime contract

- Every `/api/**` request is authenticated by `X-User-Id`.
- Seed identities: `u-1` is Alice, store-1 manager; `u-2` is Bob, store-1
  ASSOCIATE; `u-3` is Carol, store-2 ASSOCIATE.
- A caller must only see records for their store. Cross-store reads are
  reported as NOT_FOUND.
- Errors are JSON objects with `code` and `message`. Application errors use
  `AppError` subclasses; validation errors are HTTP 400.
- Repositories are in-memory. Do not imply durability across restarts.
- The established event API is `EventBus.publish(DomainEvent)` using a
  registered `EventType`; do not substitute a direct call into another module.

## Existing boundaries and checks

Feature flow is routes -> service -> repository. Feature modules may use
`common` and the read-only `staff.api` surface; do not import another feature
module's implementation. `ModuleBoundaryTest` checks module dependencies,
layer directories, and raw throws in routes/services.

Activities use `TODO`, `IN_PROGRESS`, `DONE`, and `BLOCKED`; priorities are
LOW/MEDIUM/HIGH/CRITICAL; categories are RESTOCKING, PLANOGRAM, AUDIT,
COMPLIANCE, and GENERAL. Staff roles are REGIONAL_MANAGER, STORE_MANAGER,
DEPARTMENT_LEAD, and ASSOCIATE. Store programme endpoints return `Project`
records containing `ProjectMember` role data.

Run `mvn test` for unit and integration tests, `mvn verify` for Checkstyle and
JaCoCo coverage gates, and `mvn spring-boot:run` to start on port 8080. The
coverage targets configured in `pom.xml` are 80% services, 70% routes, 60%
shared utilities, and 70% overall line coverage.

## Demonstration feature

The accepted Section 3.4 prompt is in `PROMPT.md`: shift handover bulk status
updates at `PATCH /api/activities/bulk-status`; each requested activity is
handled independently, supported target states are DONE and BLOCKED, and each
successful change creates exactly one audit entry. A DONE activity requested
as BLOCKED fails with CONFLICT, according to the Section 3.4 worked example;
successful changes publish one ACTIVITY_STATUS_CHANGED event. This behavior
is not part of the original endpoint list and must not be described as
implemented unless the application code and tests prove it.
