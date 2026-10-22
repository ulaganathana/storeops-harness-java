package com.storeops.activities.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkStatusRequest(
        @NotEmpty List<@NotBlank String> activityIds,
        @NotNull ActivityStatus status) {
}
