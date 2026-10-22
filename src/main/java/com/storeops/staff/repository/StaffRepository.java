package com.storeops.staff.repository;

import com.storeops.common.security.CurrentUser;
import com.storeops.common.security.Role;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory staff directory seeded with demo users. */
@Repository
public class StaffRepository {

    private final Map<String, CurrentUser> staff = new ConcurrentHashMap<>();

    public StaffRepository() {
        seed(new CurrentUser("u-1", "Alice Manager", "store-1", Role.STORE_MANAGER));
        seed(new CurrentUser("u-2", "Bob Associate", "store-1", Role.ASSOCIATE));
        seed(new CurrentUser("u-3", "Carol Associate", "store-2", Role.ASSOCIATE));
    }

    public Optional<CurrentUser> findById(String id) {
        return Optional.ofNullable(staff.get(id));
    }

    private void seed(CurrentUser user) {
        staff.put(user.id(), user);
    }
}
