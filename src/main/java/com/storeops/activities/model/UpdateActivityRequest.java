package com.storeops.activities.model;

public record UpdateActivityRequest(
    TaskStatus status,
    TaskPriority priority,
    TaskCategory category,
    String assigneeId) { }
