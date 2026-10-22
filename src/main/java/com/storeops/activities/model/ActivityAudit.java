package com.storeops.activities.model;

import java.time.Instant;

public record ActivityAudit(
        String activityId,
        String actorId,
        ActivityStatus previousStatus,
        ActivityStatus newStatus,
        Instant occurredAt) {
}
