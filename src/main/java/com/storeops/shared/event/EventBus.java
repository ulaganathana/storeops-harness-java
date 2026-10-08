package com.storeops.shared.event;

import java.util.Map;

/** The ONLY permitted cross-module trigger. */
public interface EventBus {
    void emit(String type, Map<String, Object> payload);
}
