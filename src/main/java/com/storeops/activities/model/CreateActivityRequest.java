package com.storeops.activities.model;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateActivityRequest(
    @NotBlank String title,
    String programmeId,
    TaskPriority priority,
    TaskCategory category,
    String assigneeId,
    LocalDate dueDate) { }
