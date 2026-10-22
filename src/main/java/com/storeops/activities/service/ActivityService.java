package com.storeops.activities.service;

import com.storeops.activities.model.Activity;
import com.storeops.activities.model.ActivityAudit;
import com.storeops.activities.model.ActivityCategory;
import com.storeops.activities.model.ActivityStatus;
import com.storeops.activities.model.BulkStatusItemResult;
import com.storeops.activities.model.BulkStatusRequest;
import com.storeops.activities.model.BulkStatusResult;
import com.storeops.activities.model.CreateActivityRequest;
import com.storeops.activities.model.Priority;
import com.storeops.activities.model.UpdateActivityRequest;
import com.storeops.activities.repository.ActivityAuditRepository;
import com.storeops.activities.repository.ActivityRepository;
import com.storeops.common.error.AppError;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.ValidationError;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventBus;
import com.storeops.common.events.EventType;
import com.storeops.common.security.CurrentUser;
import com.storeops.common.util.Ids;
import com.storeops.staff.api.StaffReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {

    private final ActivityRepository repository;
    private final ActivityAuditRepository auditRepository;
    private final StaffReader staffReader;
    private final EventBus eventBus;

    public ActivityService(ActivityRepository repository, ActivityAuditRepository auditRepository,
                           StaffReader staffReader, EventBus eventBus) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.staffReader = staffReader;
        this.eventBus = eventBus;
    }

    public List<Activity> list(CurrentUser user, String programmeId, ActivityStatus status) {
        return repository.findByStoreId(user.storeId()).stream()
                .filter(activity -> programmeId == null || programmeId.equals(activity.programmeId()))
                .filter(activity -> status == null || activity.status() == status)
                .toList();
    }

    public Activity create(CurrentUser user, CreateActivityRequest request) {
        requireAssigneeInStore(user, request.assigneeId());
        Activity activity = new Activity(
                Ids.next("act"),
                request.title(),
                request.description(),
                request.programmeId(),
                ActivityStatus.TODO,
                request.priority() != null ? request.priority() : Priority.MEDIUM,
                request.category() != null ? request.category() : ActivityCategory.GENERAL,
                request.assigneeId(),
                user.id(),
                user.storeId(),
                Instant.now());
        repository.save(activity);
        notifyAssignee(activity);
        return activity;
    }

    public Activity get(CurrentUser user, String id) {
        return repository.findById(id)
                .filter(activity -> activity.storeId().equals(user.storeId()))
                .orElseThrow(() -> new NotFoundError("activity", id));
    }

    public Activity update(CurrentUser user, String id, UpdateActivityRequest request) {
        Activity existing = get(user, id);
        requireAssigneeInStore(user, request.assigneeId());
        Activity updated = existing.withChanges(
                request.status(), request.priority(), request.category(), request.assigneeId());
        repository.save(updated);
        if (request.assigneeId() != null && !request.assigneeId().equals(existing.assigneeId())) {
            notifyAssignee(updated);
        }
        return updated;
    }

    public BulkStatusResult bulkUpdateStatus(CurrentUser user, BulkStatusRequest request) {
        if (request.status() != ActivityStatus.DONE && request.status() != ActivityStatus.BLOCKED) {
            throw new ValidationError("bulk status must be DONE or BLOCKED");
        }

        List<BulkStatusItemResult> results = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String id : request.activityIds()) {
            if (!seenIds.add(id)) {
                results.add(BulkStatusItemResult.failed(id, "CONFLICT", "activity ID is duplicated"));
                continue;
            }
            try {
                Activity existing = get(user, id);
                if (request.status() == ActivityStatus.BLOCKED && existing.status() == ActivityStatus.DONE) {
                    results.add(BulkStatusItemResult.failed(id, "CONFLICT",
                            "completed activity cannot be marked BLOCKED"));
                    continue;
                }
                Activity updated = existing.withChanges(request.status(), null, null, null);
                repository.save(updated);
                auditRepository.save(new ActivityAudit(id, user.id(), existing.status(),
                        request.status(), Instant.now()));
                eventBus.publish(DomainEvent.of(EventType.ACTIVITY_STATUS_CHANGED, updated.ownerId(),
                        "Activity " + id + " status changed from " + existing.status()
                                + " to " + request.status()));
                results.add(BulkStatusItemResult.succeeded(id));
            } catch (AppError error) {
                results.add(BulkStatusItemResult.failed(id, error.getCode(), error.getMessage()));
            }
        }
        return new BulkStatusResult(List.copyOf(results));
    }

    public List<ActivityAudit> auditHistory(String activityId) {
        return auditRepository.findByActivityId(activityId);
    }

    public void delete(CurrentUser user, String id) {
        Activity existing = get(user, id);
        if (!existing.ownerId().equals(user.id()) && !user.isStoreManager()) {
            throw new ForbiddenError("only the owner or a store manager can delete an activity");
        }
        repository.deleteById(id);
    }

    private void requireAssigneeInStore(CurrentUser user, String assigneeId) {
        if (assigneeId == null) {
            return;
        }
        CurrentUser assignee = staffReader.findById(assigneeId)
                .orElseThrow(() -> new NotFoundError("staff", assigneeId));
        if (!assignee.storeId().equals(user.storeId())) {
            throw new ValidationError("assignee belongs to a different store");
        }
    }

    private void notifyAssignee(Activity activity) {
        if (activity.assigneeId() != null) {
            eventBus.publish(DomainEvent.of(EventType.ACTIVITY_ASSIGNED, activity.assigneeId(),
                    "You were assigned activity: " + activity.title()));
        }
    }
}
