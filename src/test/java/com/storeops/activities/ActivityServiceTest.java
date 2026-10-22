package com.storeops.activities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.storeops.activities.model.Activity;
import com.storeops.activities.model.ActivityAudit;
import com.storeops.activities.model.ActivityCategory;
import com.storeops.activities.model.ActivityStatus;
import com.storeops.activities.model.BulkStatusRequest;
import com.storeops.activities.model.BulkStatusResult;
import com.storeops.activities.model.CreateActivityRequest;
import com.storeops.activities.model.Priority;
import com.storeops.activities.model.UpdateActivityRequest;
import com.storeops.activities.repository.ActivityAuditRepository;
import com.storeops.activities.repository.ActivityRepository;
import com.storeops.activities.service.ActivityService;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.ValidationError;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventType;
import com.storeops.common.events.InMemoryEventBus;
import com.storeops.common.security.CurrentUser;
import com.storeops.staff.repository.StaffRepository;
import com.storeops.staff.service.StaffService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityServiceTest {

    private final InMemoryEventBus eventBus = new InMemoryEventBus();
    private final StaffService staff = new StaffService(new StaffRepository());
    private final ActivityRepository repository = new ActivityRepository();
    private final ActivityAuditRepository auditRepository = new ActivityAuditRepository();
    private final ActivityService service = new ActivityService(repository, auditRepository, staff, eventBus);
    private final List<DomainEvent> events = new ArrayList<>();
    private final List<DomainEvent> statusEvents = new ArrayList<>();
    private final CurrentUser manager = staff.findById("u-1").orElseThrow();
    private final CurrentUser associate = staff.findById("u-2").orElseThrow();
    private final CurrentUser otherStore = staff.findById("u-3").orElseThrow();

    ActivityServiceTest() {
        eventBus.subscribe(EventType.ACTIVITY_ASSIGNED, events::add);
        eventBus.subscribe(EventType.ACTIVITY_STATUS_CHANGED, statusEvents::add);
    }

    private CreateActivityRequest request(String title, String programmeId, String assigneeId) {
        return new CreateActivityRequest(title, null, programmeId, null, null, assigneeId);
    }

    @Test
    void createAppliesDefaultsAndScopesToTheCallersStore() {
        Activity created = service.create(associate, request("Sweep", null, null));

        assertEquals(ActivityStatus.TODO, created.status());
        assertEquals(Priority.MEDIUM, created.priority());
        assertEquals(ActivityCategory.GENERAL, created.category());
        assertEquals("store-1", created.storeId());
        assertEquals("u-2", created.ownerId());
        assertTrue(events.isEmpty());
    }

    @Test
    void createWithAssigneePublishesAnEvent() {
        service.create(manager, new CreateActivityRequest("Audit", "d", "p-1",
                Priority.HIGH, ActivityCategory.COMPLIANCE, "u-2"));

        assertEquals(1, events.size());
        assertEquals("u-2", events.get(0).recipientId());
    }

    @Test
    void createRejectsAssigneesFromAnotherStoreOrUnknown() {
        assertThrows(ValidationError.class, () -> service.create(manager, request("x", null, "u-3")));
        assertThrows(NotFoundError.class, () -> service.create(manager, request("x", null, "u-999")));
    }

    @Test
    void listFiltersByStoreProgrammeAndStatus() {
        Activity a = service.create(manager, request("A", "p-1", null));
        service.create(manager, request("B", "p-2", null));
        service.create(otherStore, request("C", "p-1", null));
        service.update(manager, a.id(), new UpdateActivityRequest(ActivityStatus.DONE, null, null, null));

        assertEquals(2, service.list(manager, null, null).size());
        assertEquals(1, service.list(manager, "p-1", null).size());
        assertEquals(1, service.list(manager, null, ActivityStatus.DONE).size());
        assertEquals(0, service.list(manager, "p-2", ActivityStatus.DONE).size());
        assertEquals(1, service.list(otherStore, null, null).size());
    }

    @Test
    void getHidesActivitiesFromOtherStores() {
        Activity created = service.create(manager, request("Secret", null, null));

        assertEquals(created, service.get(associate, created.id()));
        assertThrows(NotFoundError.class, () -> service.get(otherStore, created.id()));
        assertThrows(NotFoundError.class, () -> service.get(manager, "act-missing"));
    }

    @Test
    void updateOnlyChangesProvidedFieldsAndNotifiesOnAssigneeChange() {
        Activity created = service.create(manager, request("Patch", null, null));

        Activity updated = service.update(manager, created.id(),
                new UpdateActivityRequest(null, Priority.HIGH, null, "u-2"));
        assertEquals(Priority.HIGH, updated.priority());
        assertEquals(ActivityStatus.TODO, updated.status());
        assertEquals("u-2", updated.assigneeId());
        assertEquals(1, events.size());

        service.update(manager, created.id(), new UpdateActivityRequest(ActivityStatus.DONE,
                null, ActivityCategory.RESTOCKING, "u-2"));
        assertEquals(1, events.size());
        service.update(manager, created.id(), new UpdateActivityRequest(null, null, null, null));
        assertEquals(1, events.size());
    }

    @Test
    void deleteIsAllowedForOwnerOrStoreManagerOnly() {
        Activity ownedByManager = service.create(manager, request("Mine", null, null));
        assertThrows(ForbiddenError.class, () -> service.delete(associate, ownedByManager.id()));
        service.delete(manager, ownedByManager.id());
        assertThrows(NotFoundError.class, () -> service.get(manager, ownedByManager.id()));

        Activity ownedByAssociate = service.create(associate, request("Theirs", null, null));
        service.delete(associate, ownedByAssociate.id());
        Activity another = service.create(associate, request("Again", null, null));
        service.delete(manager, another.id());
        assertTrue(service.list(manager, null, null).isEmpty());
    }

    @Test
    void bulkUpdateChangesMultipleEligibleActivitiesToDone() {
        Activity first = service.create(manager, request("First", null, null));
        Activity second = service.create(manager, request("Second", null, null));
        service.update(manager, second.id(),
                new UpdateActivityRequest(ActivityStatus.IN_PROGRESS, null, null, null));

        BulkStatusResult result = service.bulkUpdateStatus(manager,
                new BulkStatusRequest(List.of(first.id(), second.id()), ActivityStatus.DONE));

        assertEquals(List.of("SUCCEEDED", "SUCCEEDED"),
                result.results().stream().map(item -> item.outcome()).toList());
        assertEquals(ActivityStatus.DONE, service.get(manager, first.id()).status());
        assertEquals(ActivityStatus.DONE, service.get(manager, second.id()).status());
        assertEquals(ActivityStatus.TODO, auditRepository.findByActivityId(first.id()).get(0).previousStatus());
        assertEquals(ActivityStatus.IN_PROGRESS,
                auditRepository.findByActivityId(second.id()).get(0).previousStatus());
        assertEquals(2, auditRepository.findAll().size());
        assertEquals(2, statusEvents.size());
    }

    @Test
    void bulkUpdateContinuesAfterConflictAndAuditsOnlySuccesses() {
        Activity first = service.create(manager, request("First", null, null));
        Activity completed = service.create(manager, request("Completed", null, null));
        Activity last = service.create(manager, request("Last", null, null));
        service.update(manager, completed.id(),
                new UpdateActivityRequest(ActivityStatus.DONE, null, null, null));

        BulkStatusResult result = service.bulkUpdateStatus(manager,
                new BulkStatusRequest(List.of(first.id(), completed.id(), last.id()), ActivityStatus.BLOCKED));

        assertEquals(List.of("SUCCEEDED", "FAILED", "SUCCEEDED"),
                result.results().stream().map(item -> item.outcome()).toList());
        assertEquals("CONFLICT", result.results().get(1).errorCode());
        assertEquals(ActivityStatus.BLOCKED, service.get(manager, first.id()).status());
        assertEquals(ActivityStatus.DONE, service.get(manager, completed.id()).status());
        assertEquals(ActivityStatus.BLOCKED, service.get(manager, last.id()).status());
        assertEquals(2, auditRepository.findAll().size());
        ActivityAudit audit = auditRepository.findByActivityId(first.id()).get(0);
        assertEquals("u-1", audit.actorId());
        assertEquals(ActivityStatus.TODO, audit.previousStatus());
        assertEquals(ActivityStatus.BLOCKED, audit.newStatus());
        assertTrue(audit.occurredAt() != null);
        assertEquals(2, statusEvents.size());
        assertTrue(statusEvents.stream().noneMatch(event -> event.message().contains(completed.id())));
    }

    @Test
    void bulkUpdateContinuesAfterMissingItem() {
        Activity first = service.create(manager, request("First", null, null));
        Activity last = service.create(manager, request("Last", null, null));

        BulkStatusResult result = service.bulkUpdateStatus(manager,
                new BulkStatusRequest(List.of(first.id(), "act-missing", last.id()), ActivityStatus.DONE));

        assertEquals("NOT_FOUND", result.results().get(1).errorCode());
        assertEquals(ActivityStatus.DONE, service.get(manager, first.id()).status());
        assertEquals(ActivityStatus.DONE, service.get(manager, last.id()).status());
        assertEquals(2, auditRepository.findAll().size());
        assertEquals(2, statusEvents.size());
    }

    @Test
    void bulkUpdateHidesForeignStoreAndProcessesDuplicateOnce() {
        Activity foreign = service.create(otherStore, request("Foreign", null, null));
        Activity local = service.create(manager, request("Local", null, null));

        BulkStatusResult result = service.bulkUpdateStatus(manager,
                new BulkStatusRequest(List.of(foreign.id(), local.id(), local.id()), ActivityStatus.DONE));

        assertEquals("NOT_FOUND", result.results().get(0).errorCode());
        assertEquals("SUCCEEDED", result.results().get(1).outcome());
        assertEquals("CONFLICT", result.results().get(2).errorCode());
        assertEquals(ActivityStatus.TODO, service.get(otherStore, foreign.id()).status());
        assertEquals(ActivityStatus.DONE, service.get(manager, local.id()).status());
        assertEquals(1, auditRepository.findAll().size());
        assertEquals(1, statusEvents.size());
    }

    @Test
    void bulkUpdateRejectsUnsupportedStatusWithoutWritingAudit() {
        Activity activity = service.create(manager, request("Open", null, null));

        assertThrows(ValidationError.class, () -> service.bulkUpdateStatus(manager,
                new BulkStatusRequest(List.of(activity.id()), ActivityStatus.TODO)));
        assertEquals(ActivityStatus.TODO, service.get(manager, activity.id()).status());
        assertTrue(auditRepository.findAll().isEmpty());
        assertTrue(statusEvents.isEmpty());
    }
}
