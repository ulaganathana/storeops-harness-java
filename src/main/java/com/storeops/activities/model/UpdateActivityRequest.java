package com.storeops.activities.model;

public record UpdateActivityRequest(
        ActivityStatus status,
        Priority priority,
        ActivityCategory category,
        String assigneeId) {
}
