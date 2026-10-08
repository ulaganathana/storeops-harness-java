package com.storeops.programmes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.storeops.programmes.model.AddMemberRequest;
import com.storeops.programmes.model.CreateProgrammeRequest;
import com.storeops.programmes.model.Project;
import com.storeops.programmes.model.ProjectRole;
import com.storeops.programmes.repository.InMemoryProgrammeRepository;
import com.storeops.programmes.service.ProgrammeService;
import com.storeops.shared.RecordingEventBus;
import com.storeops.shared.error.ConflictError;
import com.storeops.shared.error.ForbiddenError;
import com.storeops.shared.error.NotFoundError;
import com.storeops.shared.event.EventTypes;
import com.storeops.staff.repository.InMemoryStaffRepository;
import com.storeops.staff.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgrammeServiceTest {
    private RecordingEventBus bus;
    private ProgrammeService service;

    @BeforeEach
    void setUp() {
        bus = new RecordingEventBus();
        service = new ProgrammeService(new InMemoryProgrammeRepository(), bus,
            new StaffService(new InMemoryStaffRepository()));
    }

    @Test
    void createScopesProgrammeToCallersStoreAndEmitsEvent() {
        Project p = service.create("s-1", new CreateProgrammeRequest("Summer rollout"));
        assertThat(p.getStoreId()).isEqualTo("store-1");
        assertThat(bus.ofType(EventTypes.PROGRAMME_CREATED)).hasSize(1);
        assertThat(service.listForStore("s-2")).hasSize(1);
        assertThat(service.listForStore("s-4")).isEmpty();
    }

    @Test
    void associateCannotCreateProgramme() {
        assertThatThrownBy(() -> service.create("s-3", new CreateProgrammeRequest("x")))
            .isInstanceOf(ForbiddenError.class);
    }

    @Test
    void addMemberStoresMemberAndEmitsEvent() {
        Project p = service.create("s-1", new CreateProgrammeRequest("Refit"));
        bus.events().clear();
        Project updated = service.addMember("s-1", p.getId(), new AddMemberRequest("s-3", ProjectRole.ASSOCIATE));
        assertThat(updated.getMembers()).extracting("staffId").containsExactly("s-3");
        assertThat(bus.ofType(EventTypes.MEMBER_ADDED)).hasSize(1);
        assertThat(bus.events().get(0).payload()).containsEntry("staffId", "s-3");
    }

    @Test
    void duplicateMemberIsConflict() {
        Project p = service.create("s-1", new CreateProgrammeRequest("Refit"));
        service.addMember("s-1", p.getId(), new AddMemberRequest("s-3", ProjectRole.ASSOCIATE));
        assertThatThrownBy(() -> service.addMember("s-1", p.getId(), new AddMemberRequest("s-3", ProjectRole.ASSOCIATE)))
            .isInstanceOf(ConflictError.class);
    }

    @Test
    void nonOwnerNonManagerCannotAddMember() {
        Project p = service.create("s-2", new CreateProgrammeRequest("Drive"));
        assertThatThrownBy(() -> service.addMember("s-3", p.getId(), new AddMemberRequest("s-1", ProjectRole.ASSOCIATE)))
            .isInstanceOf(ForbiddenError.class);
        assertThatThrownBy(() -> service.addMember("s-4", p.getId(), new AddMemberRequest("s-1", ProjectRole.ASSOCIATE)))
            .isInstanceOf(ForbiddenError.class);
    }

    @Test
    void addMemberToMissingProgrammeOrUnknownStaffIsNotFound() {
        assertThatThrownBy(() -> service.addMember("s-1", "nope", new AddMemberRequest("s-3", ProjectRole.ASSOCIATE)))
            .isInstanceOf(NotFoundError.class);
        Project p = service.create("s-1", new CreateProgrammeRequest("Refit"));
        assertThatThrownBy(() -> service.addMember("s-1", p.getId(), new AddMemberRequest("ghost", ProjectRole.ASSOCIATE)))
            .isInstanceOf(NotFoundError.class);
    }
}
