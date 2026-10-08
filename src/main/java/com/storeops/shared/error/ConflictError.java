package com.storeops.shared.error;

public class ConflictError extends AppError {
    private static final long serialVersionUID = 1L;

    public ConflictError(String message) {
        super("CONFLICT", message, 409);
    }
}
