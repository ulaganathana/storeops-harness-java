package com.storeops.common.error;

public class UnauthorizedError extends AppError {

    private static final long serialVersionUID = 1L;

    public UnauthorizedError(String message) {
        super("UNAUTHORIZED", message, 401);
    }
}
