package com.storeops.activities.repository;

import com.storeops.activities.model.Activity;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository {
    List<Activity> findAll();

    Optional<Activity> findById(String id);

    Activity save(Activity activity);

    boolean deleteById(String id);
}
