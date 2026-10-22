package com.storeops.activities.model;

import java.time.Instant;

public record Activity(
        String id,
        String title,
        String description,
        String programmeId,
        ActivityStatus status,
        Priority priority,
        ActivityCategory category,
        String assigneeId,
        String ownerId,
        String storeId,
        Instant createdAt) {

    /** Returns a copy with the non-null arguments applied. */
    public Activity withChanges(ActivityStatus newStatus, Priority newPriority,
                                ActivityCategory newCategory, String newAssigneeId) {
        return new Activity(id, title, description, programmeId,
                newStatus != null ? newStatus : status,
                newPriority != null ? newPriority : priority,
                newCategory != null ? newCategory : category,
                newAssigneeId != null ? newAssigneeId : assigneeId,
                ownerId, storeId, createdAt);
    }
}
