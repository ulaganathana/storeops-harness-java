# Skill: how to test StoreOps (Generator)

**Purpose:** tests must prove **business rules**, not that Spring returns 200 (FM3).

## Tooling
JUnit 5, AssertJ, Spring MockMvc (`@SpringBootTest @AutoConfigureMockMvc`, see `StoreOpsApiTest`). Unit tests construct services with
`InMemory*Repository` and the test double `com.storeops.shared.RecordingEventBus` (see `ActivityServiceTest`).

## Coverage thresholds (JaCoCo, enforced in `mvn verify`)
service packages ≥ **80%** line · controller packages ≥ **70%** · `shared.*` ≥ **60%** · overall ≥ **70%**.

## What every AC test must assert (at least one of)
1. **State:** the persisted entity/field after the action (`repo.findById(id).getStatus()`).
2. **Event:** `bus.ofType(EventTypes.X)` size and payload keys (`containsEntry("to","BLOCKED")`).
3. **Audit entry:** the audit record exists per item changed (if the feature defines one).
4. **Typed error:** `assertThatThrownBy(...).isInstanceOf(ConflictError.class)` **and**, at HTTP level, `jsonPath("$.code", is("CONFLICT"))`.
A test whose only assertion is `status().isOk()` / `isCreated()` does **not** satisfy an AC (T3 hard gate).

## Required test set per feature
- Happy path with business outcome. - Every `AppError` branch in each new service method. - Authorization branch per role used.
- Event emitted exactly once per changed item; **no** event when nothing changed.
- Partial-failure features: assert both the succeeded and failed items individually.

## Pattern
```java
@Test void blockingEmitsStatusChangedWithOwner() {
    Activity a = create("s-1", "Planogram reset"); bus.events().clear();
    service.update("s-3", a.getId(), new UpdateActivityRequest(TaskStatus.BLOCKED, null, null, null));
    assertThat(bus.ofType(EventTypes.ACTIVITY_STATUS_CHANGED)).hasSize(1);
    assertThat(bus.events().get(0).payload()).containsEntry("to", "BLOCKED").containsEntry("ownerId", "s-1");
}
```
Never add `@Disabled`/`@Ignore` (T5). Never weaken or delete an existing assertion to make a test pass.
