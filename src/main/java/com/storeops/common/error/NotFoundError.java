package com.storeops.common.error;

public class NotFoundError extends AppError {

    private static final long serialVersionUID = 1L;

    public NotFoundError(String resource, String id) {
        super("NOT_FOUND", resource + " not found: " + id, 404);
    }
}
