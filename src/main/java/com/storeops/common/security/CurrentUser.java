package com.storeops.common.security;

/** The authenticated staff member, resolved once per request. */
public record CurrentUser(String id, String name, String storeId, Role role) {

    public static final String REQUEST_ATTRIBUTE = "storeops.currentUser";

    public boolean isStoreManager() {
        return role == Role.STORE_MANAGER;
    }
}
