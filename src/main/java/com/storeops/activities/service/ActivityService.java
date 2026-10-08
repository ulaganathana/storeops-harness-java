package com.storeops.activities.service;

import com.storeops.activities.model.Activity;
import com.storeops.activities.model.CreateActivityRequest;
import com.storeops.activities.model.TaskCategory;
import com.storeops.activities.model.TaskPriority;
import com.storeops.activities.model.TaskStatus;
import com.storeops.activities.model.UpdateActivityRequest;
import com.storeops.activities.repository.ActivityRepository;
import com.storeops.shared.error.ConflictError;
import com.storeops.shared.error.ForbiddenError;
import com.storeops.shared.error.NotFoundError;
import com.storeops.shared.event.EventBus;
import com.storeops.shared.event.EventTypes;
import com.storeops.staff.model.Staff;
import com.storeops.staff.model.StaffRole;
import com.storeops.staff.service.StaffService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
    private final ActivityRepository repository;
    private final EventBus eventBus;
    private final StaffService staffService;

    public ActivityService(ActivityRepository repository, EventBus eventBus, StaffService staffService) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.staffService = staffService;
    }

    public List<Activity> list(String programmeId, TaskStatus status) {
        return repository.findAll().stream()
            .filter(a -> programmeId == null || programmeId.equals(a.getProgrammeId()))
            .filter(a -> status == null || status == a.getStatus())
            .toList();
    }

    public Activity get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundError("Activity", id));
    }

    public Activity create(String staffId, CreateActivityRequest request) {
        Staff caller = staffService.getById(staffId);
        Activity activity = new Activity();
        activity.setTitle(request.title());
        activity.setProgrammeId(request.programmeId());
        activity.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        activity.setCategory(request.category() == null ? TaskCategory.GENERAL : request.category());
        activity.setAssigneeId(request.assigneeId());
        activity.setDueDate(request.dueDate());
        activity.setOwnerId(caller.id());
        Activity saved = repository.save(activity);
        eventBus.emit(EventTypes.ACTIVITY_CREATED, Map.of("activityId", saved.getId()));
        return saved;
    }

    public Activity update(String staffId, String id, UpdateActivityRequest request) {
        staffService.getById(staffId);
        Activity activity = get(id);
        TaskStatus previous = activity.getStatus();
        if (request.status() != null && previous == TaskStatus.DONE && request.status() != TaskStatus.DONE) {
            throw new ConflictError("Activity '" + id + "' is DONE and cannot be reopened");
        }
        if (request.status() != null) {
            activity.setStatus(request.status());
        }
        if (request.priority() != null) {
            activity.setPriority(request.priority());
        }
        if (request.category() != null) {
            activity.setCategory(request.category());
        }
        if (request.assigneeId() != null) {
            activity.setAssigneeId(request.assigneeId());
        }
        Activity saved = repository.save(activity);
        if (saved.getStatus() != previous) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("activityId", saved.getId());
            payload.put("from", previous.name());
            payload.put("to", saved.getStatus().name());
            payload.put("ownerId", saved.getOwnerId());
            eventBus.emit(EventTypes.ACTIVITY_STATUS_CHANGED, payload);
        }
        return saved;
    }

    public void delete(String staffId, String id) {
        Staff caller = staffService.getById(staffId);
        Activity activity = get(id);
        boolean allowed = caller.id().equals(activity.getOwnerId()) || caller.role() == StaffRole.STORE_MANAGER;
        if (!allowed) {
            throw new ForbiddenError("Only the owner or a store manager can delete activity '" + id + "'");
        }
        repository.deleteById(id);
    }
}
