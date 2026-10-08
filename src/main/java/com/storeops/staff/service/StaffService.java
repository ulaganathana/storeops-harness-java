package com.storeops.staff.service;

import com.storeops.shared.error.NotFoundError;
import com.storeops.staff.model.Staff;
import com.storeops.staff.repository.StaffRepository;
import org.springframework.stereotype.Service;

/** Read-only lookup surface that other modules may call (never the repository). */
@Service
public class StaffService {
    private final StaffRepository repository;

    public StaffService(StaffRepository repository) {
        this.repository = repository;
    }

    public Staff getById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundError("Staff", id));
    }
}
