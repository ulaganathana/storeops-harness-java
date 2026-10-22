package com.storeops.staff.service;

import com.storeops.common.error.UnauthorizedError;
import com.storeops.common.security.CurrentUser;
import com.storeops.staff.api.StaffReader;
import com.storeops.staff.repository.StaffRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StaffService implements StaffReader {

    private final StaffRepository repository;

    public StaffService(StaffRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CurrentUser> findById(String staffId) {
        return repository.findById(staffId);
    }

    /** Stub authentication: the caller identifies itself with a staff id. */
    public CurrentUser authenticate(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            throw new UnauthorizedError("missing X-User-Id header");
        }
        return repository.findById(staffId)
                .orElseThrow(() -> new UnauthorizedError("unknown staff member: " + staffId));
    }
}
