package com.storeops.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.storeops.common.error.UnauthorizedError;
import com.storeops.common.security.CurrentUser;
import com.storeops.common.security.Role;
import com.storeops.staff.repository.StaffRepository;
import com.storeops.staff.service.StaffService;
import org.junit.jupiter.api.Test;

class StaffServiceTest {

    private final StaffService service = new StaffService(new StaffRepository());

    @Test
    void authenticateReturnsTheStaffMember() {
        CurrentUser user = service.authenticate("u-1");

        assertEquals("store-1", user.storeId());
        assertEquals(Role.STORE_MANAGER, user.role());
        assertTrue(user.isStoreManager());
    }

    @Test
    void authenticateRejectsMissingBlankAndUnknownIds() {
        assertThrows(UnauthorizedError.class, () -> service.authenticate(null));
        assertThrows(UnauthorizedError.class, () -> service.authenticate("  "));
        assertThrows(UnauthorizedError.class, () -> service.authenticate("u-999"));
    }

    @Test
    void findByIdIsReadOnlyLookup() {
        assertTrue(service.findById("u-2").isPresent());
        assertTrue(service.findById("nope").isEmpty());
    }
}
