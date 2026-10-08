package com.storeops.alerts.service;

import com.storeops.alerts.model.AlertType;
import com.storeops.alerts.model.Notification;
import com.storeops.alerts.model.NotificationChannel;
import com.storeops.alerts.model.NotificationStatus;
import com.storeops.alerts.repository.AlertRepository;
import com.storeops.shared.event.DomainEvent;
import com.storeops.shared.event.EventTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Reacts to domain events. Other modules never import this class; they emit events instead. */
@Service
public class AlertService {
    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    public List<Notification> forUser(String staffId) {
        return repository.findByRecipientId(staffId);
    }

    @EventListener
    public void onEvent(DomainEvent event) {
        Map<String, Object> p = event.payload();
        if (EventTypes.SLA_BREACH.equals(event.type()) && p.get("recipientId") != null) {
            record(String.valueOf(p.get("recipientId")), AlertType.SLA_BREACH,
                "SLA breached for activity " + p.get("activityId"));
        } else if (EventTypes.ACTIVITY_STATUS_CHANGED.equals(event.type())
            && "BLOCKED".equals(p.get("to")) && p.get("ownerId") != null) {
            record(String.valueOf(p.get("ownerId")), AlertType.ESCALATION,
                "Activity " + p.get("activityId") + " is BLOCKED");
        }
    }

    private void record(String recipientId, AlertType type, String message) {
        repository.save(new Notification(UUID.randomUUID().toString(), recipientId, type, message,
            NotificationChannel.IN_APP, NotificationStatus.UNREAD, Instant.now()));
    }
}
