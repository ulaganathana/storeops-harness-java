# StoreOps API (stub)

Retail store operations REST API. Spring Boot 3.3 / Java 21, in-memory storage, no database.

```bash
mvn verify          # checkstyle -> compile (lint-clean) -> tests -> coverage gates
mvn spring-boot:run # http://localhost:8080
```

## Authentication (staff module, auth-only)

Stub auth: send `X-User-Id` on every `/api/**` call. Seeded users:

| Id  | Name            | Store   | Role          |
|-----|-----------------|---------|---------------|
| u-1 | Alice Manager   | store-1 | STORE_MANAGER |
| u-2 | Bob Associate   | store-1 | ASSOCIATE     |
| u-3 | Carol Associate | store-2 | ASSOCIATE     |

Missing/unknown id -> `401 UNAUTHORIZED`. Data is scoped to the caller's store; other stores' records return 404.

```bash
curl -s -H 'X-User-Id: u-1' -H 'Content-Type: application/json' \
  -d '{"title":"Restock aisle 4","assigneeId":"u-2","priority":"HIGH"}' \
  localhost:8080/api/activities
```

## Endpoints (Section 3.6)

| Method + Path                   | Notes                                                                 |
|---------------------------------|-----------------------------------------------------------------------|
| GET /api/activities             | optional `programmeId`, `status` (TODO, IN_PROGRESS, DONE, BLOCKED) filters |
| POST /api/activities            | `title` required; defaults: status TODO, priority MEDIUM, category GENERAL |
| GET /api/activities/{id}        |                                                                       |
| PATCH /api/activities/{id}      | status, priority (LOW/MEDIUM/HIGH/CRITICAL), category, assigneeId       |
| DELETE /api/activities/{id}     | owner or STORE_MANAGER only, else 403; returns 204                    |
| PATCH /api/activities/bulk-status | body `{"activityIds":["..."],"status":"DONE"}`; DONE/BLOCKED only, per-item outcomes |
| GET /api/programmes             | programmes of the caller's store                                      |
| POST /api/programmes            | `name` required                                                       |
| POST /api/programmes/{id}/members | body `{"staffId": "..."}`; 409 if already a member; returns project role |
| GET /api/alerts                 | alerts for the caller                                                 |

Errors are always `{"code": "...", "message": "..."}` (NOT_FOUND, VALIDATION_ERROR, UNAUTHORIZED, FORBIDDEN, CONFLICT).

Activity categories are RESTOCKING, PLANOGRAM, AUDIT, COMPLIANCE, and GENERAL.
The staff role vocabulary is REGIONAL_MANAGER, STORE_MANAGER,
DEPARTMENT_LEAD, and ASSOCIATE. Store programme endpoints expose Project
records whose members carry a ProjectRole (STORE_MANAGER, DEPARTMENT_LEAD,
or ASSOCIATE).

The bulk status endpoint returns one ordered result per requested ID. All
successful items return 200; mixed success/failure returns 207; an all-failure
batch returns 404. Unknown and cross-store activity IDs both report
NOT_FOUND. Repeated IDs are processed once; later occurrences report
CONFLICT. Each successful update writes one process-local audit record with
actor, previous/new status, and timestamp, and publishes one
`ACTIVITY_STATUS_CHANGED` event. A DONE activity cannot be changed to
BLOCKED. Audit history is in-memory and is not durable across application
restarts.

## Layout and boundary rules

```
com.storeops
  common/      error (AppError + subclasses, GlobalExceptionHandler), events (EventBus), security, util
  staff/       api (read-only StaffReader), routes (auth interceptor), service, repository
  activities/  model, routes -> service -> repository
  programmes/  model, routes -> service -> repository
  alerts/      model, routes -> service -> repository
  reports/     layer packages only (added later as a read-only feature)
```

* Feature modules import only `common` and `staff.api` (read-only). They never import each other, so no cycles.
* Cross-module notification is event-bus only: activities/programmes publish `DomainEvent`s, alerts subscribes.
* Services/routes throw `AppError` subclasses only. Checkstyle bans `new RuntimeException(...)` and friends in main code.
* `ModuleBoundaryTest` enforces the import and no-raw-throw rules on every build.

## Quality gates

* Lint: Checkstyle (validate phase) + `javac -Xlint:all` with warnings as errors.
* Coverage (JaCoCo, `mvn verify`): service layer >= 80%, routes >= 70%, shared utils >= 60%, overall >= 70% (line coverage).
  Layer rules are evaluated per package, so each module's service/routes package must clear its threshold.
* CI independently runs `mvn -B verify` and
  `.harness/scripts/verify-disabled-tests.py`. The harness gate collector is
  `.harness/scripts/collect-gates.py`; LLM checks remain BLOCKED until the
  Evaluator supplies evidence.
