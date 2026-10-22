package com.storeops.common.events;

import java.util.function.Consumer;

public interface EventBus {

    void publish(DomainEvent event);

    void subscribe(EventType type, Consumer<DomainEvent> handler);
}
