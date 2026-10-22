package com.storeops.alerts.model;

import com.storeops.common.events.EventType;
import java.time.Instant;

public record Alert(String id, String userId, EventType type, String message, Instant createdAt, boolean read) {
}
