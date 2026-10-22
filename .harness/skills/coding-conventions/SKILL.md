# Coding conventions

- Java 21, four-space indentation, existing `com.storeops.<module>.<layer>`
  package layout, constructor injection, immutable records for transport and
  value data where suitable.
- Follow nearby naming and Spring MVC patterns. Keep routes thin and put
  validation/business rules in the service.
- Use `jakarta.validation` for request-shape constraints; enforce
  cross-field/domain rules in the service with `ValidationError`.
- Use `NotFoundError`, `ValidationError`, `ConflictError`, and
  `ForbiddenError` for their corresponding outcomes. Do not leak repository
  implementation exceptions or use raw throws in route/service packages.
- Scope every activity lookup and mutation to `CurrentUser.storeId()`. Do not
  trust a store identifier supplied in the request.
- For bulk results, retain request order, return a result for every requested
  item, and represent item failures with stable error codes and safe messages.
  A failed item must not prevent later items from being processed.
- Persist one audit record for each successful status transition, recording
  activity ID, actor ID, prior/new status, and timestamp. Do not record an
  audit entry for a failed item or a no-op unless the approved contract says
  otherwise.
- Avoid importing peer feature packages. Reuse `EventBus` only for actual
  cross-module notification; it is not the audit store.
- Add focused service tests for each branch and MockMvc tests for body,
  authentication, response shape, and store isolation. Update directly
  affected API documentation.
