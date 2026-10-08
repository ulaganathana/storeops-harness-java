# Skill: StoreOps app context (shared by all agents)

**Purpose:** the minimum facts every agent needs about StoreOps. Kept short on purpose — it is loaded by all four agents.

- **What:** retail store operations REST API. Java 17, Spring Boot 3.3, Maven, in-memory storage. Base package `com.storeops`.
- **Modules** (each has `controller / service / repository / model`): `activities`, `programmes`, `staff` (read-only to others, no CRUD API),
  `alerts`, `reports` (read-only aggregator; added by a later feature). Cross-cutting code: `com.storeops.shared.{error,event}`.
- **Base endpoints (9):** `GET|POST /api/activities`, `GET|PATCH|DELETE /api/activities/{id}`, `GET|POST /api/programmes`,
  `POST /api/programmes/{id}/members`, `GET /api/alerts`.
- **Identity:** caller is the `X-Staff-Id` header. Seed staff: `s-1` STORE_MANAGER, `s-2` DEPARTMENT_LEAD, `s-3` ASSOCIATE (all `store-1`); `s-4` STORE_MANAGER (`store-2`).
- **Domain enums:** TaskStatus TODO|IN_PROGRESS|DONE|BLOCKED; TaskPriority LOW|MEDIUM|HIGH|CRITICAL; TaskCategory RESTOCKING|PLANOGRAM|AUDIT|COMPLIANCE|GENERAL;
  StaffRole REGIONAL_MANAGER|STORE_MANAGER|DEPARTMENT_LEAD|ASSOCIATE; AlertType INVENTORY|SLA_BREACH|SHIFT_HANDOVER|ESCALATION.
- **Existing business rules:** a `DONE` activity cannot be reopened (`ConflictError`); delete = owner or STORE_MANAGER only (`ForbiddenError`);
  only STORE_MANAGER/DEPARTMENT_LEAD create programmes; duplicate programme member = `ConflictError`.
- **Events in use** (`EventTypes`): `ACTIVITY_CREATED`, `ACTIVITY_STATUS_CHANGED{activityId,from,to,ownerId}`, `PROGRAMME_CREATED`, `MEMBER_ADDED`, `SLA_BREACH{recipientId,activityId}`.
  `AlertService` listens to `DomainEvent` (`@EventListener`): `SLA_BREACH` → alert to `recipientId`; `ACTIVITY_STATUS_CHANGED` to BLOCKED → ESCALATION alert to `ownerId`.
- **The single automated check command:** `mvn -B verify` (Checkstyle → compile → tests incl. ArchUnit → JaCoCo check → SpotBugs).
