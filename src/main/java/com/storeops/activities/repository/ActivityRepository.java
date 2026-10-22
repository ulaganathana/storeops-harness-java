package com.storeops.activities.repository;

import com.storeops.activities.model.Activity;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository {

    private final Map<String, Activity> activities = new ConcurrentHashMap<>();

    public List<Activity> findByStoreId(String storeId) {
        return activities.values().stream()
                .filter(activity -> activity.storeId().equals(storeId))
                .sorted(Comparator.comparing(Activity::createdAt).thenComparing(Activity::id))
                .toList();
    }

    public Optional<Activity> findById(String id) {
        return Optional.ofNullable(activities.get(id));
    }

    public Activity save(Activity activity) {
        activities.put(activity.id(), activity);
        return activity;
    }

    public void deleteById(String id) {
        activities.remove(id);
    }
}
