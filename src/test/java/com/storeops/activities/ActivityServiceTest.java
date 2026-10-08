package com.storeops.activities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.storeops.activities.model.Activity;
import com.storeops.activities.model.CreateActivityRequest;
import com.storeops.activities.model.TaskCategory;
import com.storeops.activities.model.TaskPriority;
import com.storeops.activities.model.TaskStatus;
import com.storeops.activities.model.UpdateActivityRequest;
import com.storeops.activities.repository.InMemoryActivityRepository;
import com.storeops.activities.service.ActivityService;
import com.storeops.shared.RecordingEventBus;
import com.storeops.shared.error.ConflictError;
import com.storeops.shared.error.ForbiddenError;
import com.storeops.shared.error.NotFoundError;
import com.storeops.shared.event.EventTypes;
import com.storeops.staff.repository.InMemoryStaffRepository;
import com.storeops.staff.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityServiceTest {
    private RecordingEventBus bus;
    private InMemoryActivityRepository repo;
    private ActivityService service;

    @BeforeEach
    void setUp() {
        bus = new RecordingEventBus();
        repo = new InMemoryActivityRepository();
        service = new ActivityService(repo, bus, new StaffService(new InMemoryStaffRepository()));
    }

    private Activity create(String staff, String title) {
        return service.create(staff, new CreateActivityRequest(title, "p-1", null, null, "s-3", null));
    }

    @Test
    void createAppliesDefaultsSetsOwnerAndEmitsEvent() {
        Activity a = create("s-2", "Restock aisle 4");
        assertThat(a.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(a.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(a.getCategory()).isEqualTo(TaskCategory.GENERAL);
        assertThat(a.getOwnerId()).isEqualTo("s-2");
        assertThat(bus.ofType(EventTypes.ACTIVITY_CREATED)).hasSize(1);
        assertThat(bus.events().get(0).payload()).containsEntry("activityId", a.getId());
    }

    @Test
    void createKeepsExplicitPriorityAndCategory() {
        Activity a = service.create("s-1", new CreateActivityRequest("Audit", null, TaskPriority.CRITICAL,
            TaskCategory.AUDIT, null, null));
        assertThat(a.getPriority()).isEqualTo(TaskPriority.CRITICAL);
        assertThat(a.getCategory()).isEqualTo(TaskCategory.AUDIT);
    }

    @Test
    void createWithUnknownStaffIsNotFound() {
        assertThatThrownBy(() -> create("nobody", "x")).isInstanceOf(NotFoundError.class);
        assertThat(repo.findAll()).isEmpty();
    }

    @Test
    void statusChangeEmitsEventWithFromToAndOwner() {
        Activity a = create("s-1", "Planogram reset");
        bus.events().clear();
        service.update("s-3", a.getId(), new UpdateActivityRequest(TaskStatus.BLOCKED, null, null, null));
        assertThat(bus.ofType(EventTypes.ACTIVITY_STATUS_CHANGED)).hasSize(1);
        assertThat(bus.events().get(0).payload())
            .containsEntry("from", "TODO").containsEntry("to", "BLOCKED").containsEntry("ownerId", "s-1");
    }

    @Test
    void updateWithoutStatusChangeEmitsNoEventButAppliesFields() {
        Activity a = create("s-1", "Compliance check");
        bus.events().clear();
        Activity updated = service.update("s-1", a.getId(),
            new UpdateActivityRequest(null, TaskPriority.HIGH, TaskCategory.COMPLIANCE, "s-2"));
        assertThat(updated.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(updated.getCategory()).isEqualTo(TaskCategory.COMPLIANCE);
        assertThat(updated.getAssigneeId()).isEqualTo("s-2");
        assertThat(bus.events()).isEmpty();
    }

    @Test
    void doneActivityCannotBeReopened() {
        Activity a = create("s-1", "Done task");
        service.update("s-1", a.getId(), new UpdateActivityRequest(TaskStatus.DONE, null, null, null));
        assertThatThrownBy(() -> service.update("s-1", a.getId(),
            new UpdateActivityRequest(TaskStatus.TODO, null, null, null))).isInstanceOf(ConflictError.class);
        assertThat(service.get(a.getId()).getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void getMissingIsNotFound() {
        assertThatThrownBy(() -> service.get("nope")).isInstanceOf(NotFoundError.class);
    }

    @Test
    void listFiltersByProgrammeAndStatus() {
        Activity a = create("s-1", "A");
        service.create("s-1", new CreateActivityRequest("B", "p-2", null, null, null, null));
        service.update("s-1", a.getId(), new UpdateActivityRequest(TaskStatus.IN_PROGRESS, null, null, null));
        assertThat(service.list(null, null)).hasSize(2);
        assertThat(service.list("p-1", null)).extracting(Activity::getTitle).containsExactly("A");
        assertThat(service.list(null, TaskStatus.IN_PROGRESS)).hasSize(1);
        assertThat(service.list("p-2", TaskStatus.IN_PROGRESS)).isEmpty();
    }

    @Test
    void ownerCanDeleteOwnActivity() {
        Activity a = create("s-2", "Mine");
        service.delete("s-2", a.getId());
        assertThat(repo.findById(a.getId())).isEmpty();
    }

    @Test
    void storeManagerCanDeleteAnyActivity() {
        Activity a = create("s-2", "Lead's");
        service.delete("s-1", a.getId());
        assertThat(repo.findAll()).isEmpty();
    }

    @Test
    void associateCannotDeleteSomeoneElsesActivity() {
        Activity a = create("s-2", "Lead's");
        assertThatThrownBy(() -> service.delete("s-3", a.getId())).isInstanceOf(ForbiddenError.class);
        assertThat(repo.findById(a.getId())).isPresent();
    }
}
