package com.storeops.common.error;

/**
 * Base class for every error raised by services and routes.
 * Raw exceptions must never be thrown from application code.
 */
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
