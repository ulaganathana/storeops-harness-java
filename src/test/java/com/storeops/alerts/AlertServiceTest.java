package com.storeops.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import com.storeops.alerts.model.AlertType;
import com.storeops.alerts.model.Notification;
import com.storeops.alerts.repository.InMemoryAlertRepository;
import com.storeops.alerts.service.AlertService;
import com.storeops.shared.event.DomainEvent;
import com.storeops.shared.event.EventTypes;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertServiceTest {
    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(new InMemoryAlertRepository());
    }

    private DomainEvent event(String type, Map<String, Object> payload) {
        return new DomainEvent(type, payload, Instant.now());
    }

    @Test
    void slaBreachCreatesAlertForRecipientOnly() {
        service.onEvent(event(EventTypes.SLA_BREACH, Map.of("recipientId", "s-2", "activityId", "a-1")));
        assertThat(service.forUser("s-2")).hasSize(1);
        assertThat(service.forUser("s-2").get(0).type()).isEqualTo(AlertType.SLA_BREACH);
        assertThat(service.forUser("s-1")).isEmpty();
    }

    @Test
    void blockedStatusChangeEscalatesToOwner() {
        service.onEvent(event(EventTypes.ACTIVITY_STATUS_CHANGED,
            Map.of("to", "BLOCKED", "ownerId", "s-1", "activityId", "a-1")));
        Notification n = service.forUser("s-1").get(0);
        assertThat(n.type()).isEqualTo(AlertType.ESCALATION);
        assertThat(n.message()).contains("a-1");
    }

    @Test
    void unrelatedEventsAndNonBlockedChangesCreateNoAlerts() {
        service.onEvent(event(EventTypes.ACTIVITY_CREATED, Map.of("activityId", "a-1")));
        service.onEvent(event(EventTypes.ACTIVITY_STATUS_CHANGED, Map.of("to", "DONE", "ownerId", "s-1")));
        service.onEvent(event(EventTypes.SLA_BREACH, Map.of("activityId", "a-1")));
        assertThat(service.forUser("s-1")).isEmpty();
    }
}
