package com.storeops.shared.event;

import java.time.Instant;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventBus implements EventBus {
    private final ApplicationEventPublisher publisher;

    public SpringEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void emit(String type, Map<String, Object> payload) {
        publisher.publishEvent(new DomainEvent(type, Map.copyOf(payload), Instant.now()));
    }
}
