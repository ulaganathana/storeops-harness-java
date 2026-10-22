package com.storeops.alerts.service;

import com.storeops.alerts.model.Alert;
import com.storeops.alerts.repository.AlertRepository;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventBus;
import com.storeops.common.events.EventType;
import com.storeops.common.util.Ids;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Service;

/** Turns domain events into per-user alerts. Other modules reach it only through the event bus. */
@Service
public class AlertService {

    private final AlertRepository repository;
    private final EventBus eventBus;

    public AlertService(AlertRepository repository, EventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void registerSubscriptions() {
        eventBus.subscribe(EventType.ACTIVITY_ASSIGNED, this::onEvent);
        eventBus.subscribe(EventType.ACTIVITY_STATUS_CHANGED, this::onEvent);
        eventBus.subscribe(EventType.PROGRAMME_MEMBER_ADDED, this::onEvent);
    }

    public List<Alert> listForUser(String userId) {
        return repository.findByUserId(userId);
    }

    private void onEvent(DomainEvent event) {
        repository.save(new Alert(Ids.next("alt"), event.recipientId(), event.type(),
                event.message(), event.occurredAt(), false));
    }
}
