# Skill: StoreOps coding conventions (Generator)

**Purpose:** concrete Java/Spring rules for new StoreOps code. Follow the existing activities module as the reference implementation.

## Package layout
`com.storeops.<module>.{controller,service,repository,model}`. New module = same four sub-packages. Shared code only in `com.storeops.shared.{error,event}`.
Repository = interface `XRepository` + `InMemoryXRepository` (`@Repository`). Tests mirror packages under `src/test/java/com/storeops/<module>/`.

## Naming
`XController`, `XService`, `XRepository`, request records `CreateXRequest`/`UpdateXRequest`/`BulkXRequest`, enums singular upper-snake values.
Event types are `UPPER_SNAKE` constants in `EventTypes`.

## Layer responsibilities
- **controller:** `@RestController`, `@RequestMapping("/api/<module>")`, read `@RequestHeader("X-Staff-Id")`, `@Valid @RequestBody`, delegate to **one** service call. No `if` business rules.
- **service:** all business rules, authorization by `StaffRole`, validation beyond bean validation, event emission. Constructor injection only.
- **repository:** data access only, in-memory `ConcurrentHashMap`. No HTTP types, no service calls, no events.

## Errors (FM2)
```java
return repository.findById(id).orElseThrow(() -> new NotFoundError("Activity", id));   // 404 NOT_FOUND
throw new ConflictError("Activity '" + id + "' is DONE and cannot be reopened");       // 409 CONFLICT
throw new ForbiddenError("Only the owner or a store manager can delete ...");          // 403 FORBIDDEN
throw new ValidationError("ids must not be empty");                                    // 400 VALIDATION_ERROR
```
Need a new error kind? Add a subclass of `AppError` in `shared.error` — never throw a raw exception, never catch-and-rethrow as `RuntimeException`.
`GlobalExceptionHandler` is the only place that maps errors to HTTP.

## Events (FM4)
```java
Map<String,Object> payload = new HashMap<>();
payload.put("activityId", saved.getId()); payload.put("from", prev.name()); payload.put("to", saved.getStatus().name());
eventBus.emit(EventTypes.ACTIVITY_STATUS_CHANGED, payload);
```
Emit **after** the repository save succeeds. `Map.of` rejects nulls — use `HashMap` when a value may be null. Consumers use `@EventListener` on `DomainEvent`
inside their own module; the emitting module never references the consumer.

## Cross-module reads
Allowed: `staffService.getById(id)` (service layer, read-only). Forbidden: any `*.repository` of another module (FM1).

## Static analysis you must keep clean
Checkstyle (`UnusedImports`, `AvoidStarImport`, `RawThrow`), SpotBugs (threshold High). No `System.out`, no wildcard imports, no unused imports. Do not edit `config/` or `pom.xml`.
