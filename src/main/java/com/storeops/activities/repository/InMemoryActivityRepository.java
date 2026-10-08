package com.storeops.activities.repository;

import com.storeops.activities.model.Activity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryActivityRepository implements ActivityRepository {
    private final Map<String, Activity> store = new ConcurrentHashMap<>();

    @Override
    public List<Activity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Activity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Activity save(Activity activity) {
        if (activity.getId() == null) {
            activity.setId(UUID.randomUUID().toString());
        }
        store.put(activity.getId(), activity);
        return activity;
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }
}
