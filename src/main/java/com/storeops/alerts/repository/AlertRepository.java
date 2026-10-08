package com.storeops.alerts.repository;

import com.storeops.alerts.model.Notification;
import java.util.List;

public interface AlertRepository {
    Notification save(Notification notification);

    List<Notification> findByRecipientId(String recipientId);
}
