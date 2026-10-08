package com.storeops.staff.repository;

import com.storeops.staff.model.Staff;
import com.storeops.staff.model.StaffRole;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Seeded, read-only data. Staff is read-only for every other module. */
@Repository
public class InMemoryStaffRepository implements StaffRepository {
    private final Map<String, Staff> store;

    public InMemoryStaffRepository() {
        store = new ConcurrentHashMap<>(List.of(
            new Staff("s-1", "Sam Manager", "store-1", StaffRole.STORE_MANAGER),
            new Staff("s-2", "Dana Lead", "store-1", StaffRole.DEPARTMENT_LEAD),
            new Staff("s-3", "Alex Associate", "store-1", StaffRole.ASSOCIATE),
            new Staff("s-4", "Priya Manager", "store-2", StaffRole.STORE_MANAGER))
            .stream().collect(Collectors.toMap(Staff::id, s -> s)));
    }

    @Override
    public Optional<Staff> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
