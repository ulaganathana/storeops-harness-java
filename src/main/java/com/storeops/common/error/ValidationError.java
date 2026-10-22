package com.storeops.common.error;

public class ValidationError extends AppError {

    private static final long serialVersionUID = 1L;

    public ValidationError(String message) {
        super("VALIDATION_ERROR", message, 400);
    }
}
