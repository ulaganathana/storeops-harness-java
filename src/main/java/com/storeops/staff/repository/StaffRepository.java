package com.storeops.staff.repository;

import com.storeops.staff.model.Staff;
import java.util.Optional;

public interface StaffRepository {
    Optional<Staff> findById(String id);
}
