package com.storeops.activities.model;

import jakarta.validation.constraints.NotBlank;

public record CreateActivityRequest(
        @NotBlank String title,
        String description,
        String programmeId,
        Priority priority,
        ActivityCategory category,
        String assigneeId) {
}
