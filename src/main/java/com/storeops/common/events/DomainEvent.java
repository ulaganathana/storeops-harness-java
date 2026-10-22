package com.storeops.common.events;

import java.time.Instant;

/** Notification published on the event bus; the only way modules notify each other. */
public record DomainEvent(EventType type, String recipientId, String message, Instant occurredAt) {

    public static DomainEvent of(EventType type, String recipientId, String message) {
        return new DomainEvent(type, recipientId, message, Instant.now());
    }
}
