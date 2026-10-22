package com.storeops.common.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Synchronous in-process event bus. */
@Component
public class InMemoryEventBus implements EventBus {

    private final Map<EventType, List<Consumer<DomainEvent>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
        handlers.getOrDefault(event.type(), List.of()).forEach(handler -> handler.accept(event));
    }

    @Override
    public void subscribe(EventType type, Consumer<DomainEvent> handler) {
        handlers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(handler);
    }
}
