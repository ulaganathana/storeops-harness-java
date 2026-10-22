package com.storeops.alerts.model;

import java.time.Instant;

public record Notification(
        String id,
        String userId,
        AlertType type,
        NotificationChannel channel,
        NotificationStatus status,
        String message,
        Instant createdAt) {
}
