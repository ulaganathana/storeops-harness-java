package com.storeops.common.error;

public class ForbiddenError extends AppError {

    private static final long serialVersionUID = 1L;

    public ForbiddenError(String message) {
        super("FORBIDDEN", message, 403);
    }
}
