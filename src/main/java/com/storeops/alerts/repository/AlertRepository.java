package com.storeops.alerts.repository;

import com.storeops.alerts.model.Alert;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AlertRepository {

    private final Map<String, Alert> alerts = new ConcurrentHashMap<>();

    public Alert save(Alert alert) {
        alerts.put(alert.id(), alert);
        return alert;
    }

    public List<Alert> findByUserId(String userId) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .sorted(Comparator.comparing(Alert::createdAt).thenComparing(Alert::id))
                .toList();
    }
}
