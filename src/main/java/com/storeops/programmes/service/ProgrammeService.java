package com.storeops.programmes.service;

import com.storeops.common.error.ConflictError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.ValidationError;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventBus;
import com.storeops.common.events.EventType;
import com.storeops.common.security.CurrentUser;
import com.storeops.common.util.Ids;
import com.storeops.programmes.model.CreateProgrammeRequest;
import com.storeops.programmes.model.Project;
import com.storeops.programmes.model.ProjectMember;
import com.storeops.programmes.model.ProjectRole;
import com.storeops.programmes.repository.ProgrammeRepository;
import com.storeops.staff.api.StaffReader;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProgrammeService {

    private final ProgrammeRepository repository;
    private final StaffReader staffReader;
    private final EventBus eventBus;

    public ProgrammeService(ProgrammeRepository repository, StaffReader staffReader, EventBus eventBus) {
        this.repository = repository;
        this.staffReader = staffReader;
        this.eventBus = eventBus;
    }

    public List<Project> list(CurrentUser user) {
        return repository.findByStoreId(user.storeId());
    }

    public Project create(CurrentUser user, CreateProgrammeRequest request) {
        Project programme = new Project(
                Ids.next("prg"), request.name(), request.description(), user.storeId(), List.of());
        return repository.save(programme);
    }

    public Project addMember(CurrentUser user, String programmeId, String staffId) {
        Project programme = repository.findById(programmeId)
                .filter(found -> found.storeId().equals(user.storeId()))
                .orElseThrow(() -> new NotFoundError("programme", programmeId));
        CurrentUser member = staffReader.findById(staffId)
                .orElseThrow(() -> new NotFoundError("staff", staffId));
        if (!member.storeId().equals(programme.storeId())) {
            throw new ValidationError("staff member belongs to a different store");
        }
        if (programme.hasMember(staffId)) {
            throw new ConflictError("staff member is already in programme " + programmeId);
        }
        ProjectRole role = projectRoleFor(member);
        Project updated = repository.save(programme.withMember(new ProjectMember(staffId, role)));
        eventBus.publish(DomainEvent.of(EventType.PROGRAMME_MEMBER_ADDED, staffId,
                "You were added to programme: " + programme.name()));
        return updated;
    }

    private ProjectRole projectRoleFor(CurrentUser member) {
        return switch (member.role()) {
            case STORE_MANAGER -> ProjectRole.STORE_MANAGER;
            case DEPARTMENT_LEAD -> ProjectRole.DEPARTMENT_LEAD;
            case ASSOCIATE -> ProjectRole.ASSOCIATE;
            case REGIONAL_MANAGER -> throw new ValidationError(
                    "regional managers cannot be added to a store programme");
        };
    }
}
