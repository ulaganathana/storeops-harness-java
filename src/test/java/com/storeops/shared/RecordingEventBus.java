package com.storeops.shared;

import com.storeops.shared.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Test double: captures every emitted event so tests can assert business side effects. */
public class RecordingEventBus implements EventBus {
    public record Emitted(String type, Map<String, Object> payload) { }

    private final List<Emitted> events = new ArrayList<>();

    @Override
    public void emit(String type, Map<String, Object> payload) {
        events.add(new Emitted(type, payload));
    }

    public List<Emitted> events() {
        return events;
    }

    public List<Emitted> ofType(String type) {
        return events.stream().filter(e -> e.type().equals(type)).toList();
    }
}
