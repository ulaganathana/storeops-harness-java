package com.storeops.programmes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.storeops.common.error.ConflictError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.ValidationError;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventType;
import com.storeops.common.events.InMemoryEventBus;
import com.storeops.common.security.CurrentUser;
import com.storeops.programmes.model.CreateProgrammeRequest;
import com.storeops.programmes.model.Project;
import com.storeops.programmes.model.ProjectRole;
import com.storeops.programmes.repository.ProgrammeRepository;
import com.storeops.programmes.service.ProgrammeService;
import com.storeops.staff.repository.StaffRepository;
import com.storeops.staff.service.StaffService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgrammeServiceTest {

    private final InMemoryEventBus eventBus = new InMemoryEventBus();
    private final StaffService staff = new StaffService(new StaffRepository());
    private final ProgrammeService service = new ProgrammeService(new ProgrammeRepository(), staff, eventBus);
    private final List<DomainEvent> events = new ArrayList<>();
    private final CurrentUser manager = staff.findById("u-1").orElseThrow();
    private final CurrentUser otherStore = staff.findById("u-3").orElseThrow();

    ProgrammeServiceTest() {
        eventBus.subscribe(EventType.PROGRAMME_MEMBER_ADDED, events::add);
    }

    @Test
    void createAndListAreScopedToTheCallersStore() {
        service.create(manager, new CreateProgrammeRequest("Zeta", "d"));
        service.create(manager, new CreateProgrammeRequest("Alpha", "d"));
        service.create(otherStore, new CreateProgrammeRequest("Elsewhere", "d"));

        List<Project> storeOne = service.list(manager);
        assertEquals(2, storeOne.size());
        assertEquals("Alpha", storeOne.get(0).name());
        assertEquals(1, service.list(otherStore).size());
    }

    @Test
    void addMemberPublishesAnEventAndRejectsInvalidRequests() {
        Project programme = service.create(manager, new CreateProgrammeRequest("Training", "d"));

        Project updated = service.addMember(manager, programme.id(), "u-2");
        assertTrue(updated.hasMember("u-2"));
        assertEquals(ProjectRole.ASSOCIATE, updated.members().get(0).role());
        assertEquals(1, events.size());
        assertEquals("u-2", events.get(0).recipientId());

        assertThrows(ConflictError.class, () -> service.addMember(manager, programme.id(), "u-2"));
        assertThrows(ValidationError.class, () -> service.addMember(manager, programme.id(), "u-3"));
        assertThrows(NotFoundError.class, () -> service.addMember(manager, programme.id(), "u-999"));
        assertThrows(NotFoundError.class, () -> service.addMember(manager, "prg-missing", "u-2"));
        assertThrows(NotFoundError.class, () -> service.addMember(otherStore, programme.id(), "u-3"));
        assertEquals(1, events.size());
    }
}
