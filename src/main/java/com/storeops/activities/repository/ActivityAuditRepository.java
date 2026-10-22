package com.storeops.activities.repository;

import com.storeops.activities.model.ActivityAudit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityAuditRepository {

    private final List<ActivityAudit> audits = new CopyOnWriteArrayList<>();

    public ActivityAudit save(ActivityAudit audit) {
        audits.add(audit);
        return audit;
    }

    public List<ActivityAudit> findByActivityId(String activityId) {
        return audits.stream()
                .filter(audit -> audit.activityId().equals(activityId))
                .toList();
    }

    public List<ActivityAudit> findAll() {
        return List.copyOf(audits);
    }
}
