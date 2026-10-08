package com.storeops.shared.error;

public class NotFoundError extends AppError {
    private static final long serialVersionUID = 1L;

    public NotFoundError(String entity, String id) {
        super("NOT_FOUND", entity + " '" + id + "' not found", 404);
    }
}
