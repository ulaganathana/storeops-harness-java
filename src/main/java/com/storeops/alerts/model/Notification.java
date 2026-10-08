package com.storeops.alerts.model;

import java.time.Instant;

public record Notification(String id, String recipientId, AlertType type, String message,
                           NotificationChannel channel, NotificationStatus status, Instant createdAt) { }
