package com.storeops.shared.event;

import java.time.Instant;
import java.util.Map;

public record DomainEvent(String type, Map<String, Object> payload, Instant occurredAt) { }
