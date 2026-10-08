# Skill: StoreOps API contract (Generator)

**Purpose:** how HTTP concerns are done in StoreOps so new endpoints look like existing ones.

- **Auth:** mutating endpoints require header `X-Staff-Id`; missing header → 400 `VALIDATION_ERROR` (handled globally). Reads of activities do not require it.
- **Errors:** body is always `{"code": "...", "message": "..."}` from `ErrorResponse`; never return ad-hoc error bodies, never catch `AppError` in a controller.
- **Status codes:** create → 201; update/get/list → 200; delete → 204; `NotFoundError` 404; `ValidationError`/bean validation/bad enum 400; `ForbiddenError` 403; `ConflictError` 409.
- **Bulk / partial-failure endpoints:** return **207 Multi-Status** with `{"results":[{"id","status":"SUCCEEDED|FAILED","error":{"code","message"}|null}], "succeeded":n, "failed":m}`;
  per-item failures are reported as typed codes (`CONFLICT`, `NOT_FOUND`) and must not abort other items. Validate the request envelope (non-empty list, max 100 ids) with `ValidationError`.
- **Validation:** bean validation annotations on request records (`@NotBlank`, `@NotNull`); rules that need data live in the service.
- **Path ordering:** a literal path like `/api/activities/bulk-status` must not be shadowed by `/{id}`; Spring prefers literal matches, but add an API test proving it.
- **DTOs:** Java `record`s in `model`; entities are returned directly only for existing endpoints — new endpoints return dedicated response records.
- **Running locally:** `./mvnw spring-boot:run` → `http://localhost:8080`; container: `docker compose up --build`.
