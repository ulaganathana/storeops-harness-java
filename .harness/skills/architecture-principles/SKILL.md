# StoreOps architecture principles

1. **Keep feature ownership local.** Routes validate and map HTTP, services own
   business rules, repositories own persistence. Do not move service logic
   into controllers.
2. **Keep feature modules independent.** A feature can use `common` and the
   read-only `staff.api`; it cannot depend on another feature module's
   repositories, services, routes, or models. Cross-module notifications use
   the shared event bus.
3. **Use application errors.** Throw an appropriate `AppError` subclass from
   application layers. Do not throw raw Java exceptions from routes/services.
4. **Enforce store isolation at the service boundary.** Authenticate each
   request and scope reads and writes to the caller's store. Avoid revealing
   whether a foreign-store record exists.
5. **Make side effects observable and minimal.** A committed activity change
   has one corresponding audit record; failed items have none. Event
   publication is not, by itself, durable audit storage.
6. **Keep deterministic contracts.** Preserve caller request order in batch
   results, define duplicate-ID behavior, and make each result independently
   interpretable without relying on log order.

Use the existing package names and helpers before adding abstractions. Any
intentional relaxation must be specified and approved before implementation.
