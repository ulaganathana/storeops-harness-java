package com.storeops.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.storeops.alerts.model.Alert;
import com.storeops.alerts.repository.AlertRepository;
import com.storeops.alerts.service.AlertService;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventType;
import com.storeops.common.events.InMemoryEventBus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertServiceTest {

    private final InMemoryEventBus eventBus = new InMemoryEventBus();
    private final AlertService service = new AlertService(new AlertRepository(), eventBus);

    @Test
    void eventsBecomeUnreadAlertsForTheirRecipientOnly() {
        service.registerSubscriptions();

        eventBus.publish(DomainEvent.of(EventType.ACTIVITY_ASSIGNED, "u-2", "first"));
        eventBus.publish(DomainEvent.of(EventType.PROGRAMME_MEMBER_ADDED, "u-2", "second"));
        eventBus.publish(DomainEvent.of(EventType.ACTIVITY_STATUS_CHANGED, "u-2", "third"));
        eventBus.publish(DomainEvent.of(EventType.ACTIVITY_ASSIGNED, "u-1", "other"));

        List<Alert> alerts = service.listForUser("u-2");
        assertEquals(3, alerts.size());
        assertFalse(alerts.get(0).read());
        assertEquals(1, service.listForUser("u-1").size());
        assertTrue(service.listForUser("u-3").isEmpty());
    }
}
