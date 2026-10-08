package com.storeops.alerts.repository;

import com.storeops.alerts.model.Notification;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAlertRepository implements AlertRepository {
    private final List<Notification> store = new CopyOnWriteArrayList<>();

    @Override
    public Notification save(Notification notification) {
        store.add(notification);
        return notification;
    }

    @Override
    public List<Notification> findByRecipientId(String recipientId) {
        return store.stream().filter(n -> n.recipientId().equals(recipientId)).toList();
    }
}
