package com.storeops.programmes.service;

import com.storeops.programmes.model.AddMemberRequest;
import com.storeops.programmes.model.CreateProgrammeRequest;
import com.storeops.programmes.model.Project;
import com.storeops.programmes.model.ProjectMember;
import com.storeops.programmes.repository.ProgrammeRepository;
import com.storeops.shared.error.ConflictError;
import com.storeops.shared.error.ForbiddenError;
import com.storeops.shared.error.NotFoundError;
import com.storeops.shared.event.EventBus;
import com.storeops.shared.event.EventTypes;
import com.storeops.staff.model.Staff;
import com.storeops.staff.model.StaffRole;
import com.storeops.staff.service.StaffService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProgrammeService {
    private final ProgrammeRepository repository;
    private final EventBus eventBus;
    private final StaffService staffService;

    public ProgrammeService(ProgrammeRepository repository, EventBus eventBus, StaffService staffService) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.staffService = staffService;
    }

    public List<Project> listForStore(String staffId) {
        return repository.findByStoreId(staffService.getById(staffId).storeId());
    }

    public Project create(String staffId, CreateProgrammeRequest request) {
        Staff caller = staffService.getById(staffId);
        if (caller.role() != StaffRole.STORE_MANAGER && caller.role() != StaffRole.DEPARTMENT_LEAD) {
            throw new ForbiddenError("Only store managers and department leads can create programmes");
        }
        Project project = new Project();
        project.setName(request.name());
        project.setStoreId(caller.storeId());
        project.setOwnerId(caller.id());
        Project saved = repository.save(project);
        eventBus.emit(EventTypes.PROGRAMME_CREATED, Map.of("programmeId", saved.getId()));
        return saved;
    }

    public Project addMember(String staffId, String programmeId, AddMemberRequest request) {
        Staff caller = staffService.getById(staffId);
        Project project = repository.findById(programmeId)
            .orElseThrow(() -> new NotFoundError("Programme", programmeId));
        boolean allowed = caller.id().equals(project.getOwnerId())
            || (caller.role() == StaffRole.STORE_MANAGER && caller.storeId().equals(project.getStoreId()));
        if (!allowed) {
            throw new ForbiddenError("Only the programme owner or a store manager of the store can add members");
        }
        Staff target = staffService.getById(request.staffId());
        boolean duplicate = project.getMembers().stream().anyMatch(m -> m.staffId().equals(target.id()));
        if (duplicate) {
            throw new ConflictError("Staff '" + target.id() + "' is already a member of programme '" + programmeId + "'");
        }
        project.getMembers().add(new ProjectMember(target.id(), request.role()));
        Project saved = repository.save(project);
        eventBus.emit(EventTypes.MEMBER_ADDED, Map.of("programmeId", programmeId, "staffId", target.id()));
        return saved;
    }
}
