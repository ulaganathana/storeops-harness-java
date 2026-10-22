package com.storeops.staff.api;

import com.storeops.common.security.CurrentUser;
import java.util.Optional;

/**
 * The only staff type other modules may import. Deliberately read-only:
 * no create/update/delete operations are exposed outside the staff module.
 */
public interface StaffReader {

    Optional<CurrentUser> findById(String staffId);
}
