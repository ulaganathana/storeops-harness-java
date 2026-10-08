# Skill: StoreOps architecture principles (shared by all agents)

**Purpose:** the non-negotiable architecture rules. Each maps to a failure mode seen in the client's earlier AI experiment and to a gate.

| Rule | Statement | FM | Gate |
|---|---|---|---|
| R1 Module boundary | No class outside `com.storeops.X..` may import `com.storeops.X.repository..`. Cross-module **reads** call `X.service` only. | FM1 | A1 (ArchUnit) |
| R2 Error contract | In `..service..` and `..controller..` never `throw new RuntimeException/Exception/IllegalStateException/IllegalArgumentException/Error`. Use `NotFoundError`(404) `ValidationError`(400) `ForbiddenError`(403) `ConflictError`(409), all `extends AppError(code,message,statusCode)`. | FM2 | A2 (Checkstyle `RawThrow` + ArchUnit) |
| R3 Event bus only | Cross-module **side effects** are `eventBus.emit(EventTypes.X, payload)`. Never import/call `AlertService` or any `reports` class from another module. New event types are added to `EventTypes` first. | FM4 | A3 (ArchUnit), A6 |
| R4 Layer separation | controller → service → repository. Controllers contain no business logic and never touch repositories; repositories never use HTTP types or call other services. | — | A4 (ArchUnit) |
| R5 Read-only reports | `reports` aggregates via other modules' **service read methods** and never calls a write method (`save/create/update/delete/add*`) on them. | — | A5 (LLM, hard) |
| R6 Business-outcome tests | Every acceptance criterion has a test asserting state / emitted events / audit entries, not only `status().isOk()`. | FM3 | T3 (LLM, hard) |

**Example (R1+R3 together).** SLA breach from `activities`:
`eventBus.emit(EventTypes.SLA_BREACH, Map.of("recipientId", leadId, "activityId", id));` ✔
`alertService.notify(...)` or `new InMemoryAlertRepository().save(...)` inside `activities` ✘ (A3/A1 hard-gate failure).
