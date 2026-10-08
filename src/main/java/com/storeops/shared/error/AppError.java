package com.storeops.shared.error;

/** Base of the typed error hierarchy. Services and controllers throw subclasses of this, never raw exceptions. */
public abstract class AppError extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;
    private final int statusCode;

    protected AppError(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
