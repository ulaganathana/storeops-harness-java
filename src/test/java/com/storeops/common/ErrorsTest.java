package com.storeops.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.storeops.common.error.AppError;
import com.storeops.common.error.ApiError;
import com.storeops.common.error.ConflictError;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.GlobalExceptionHandler;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.UnauthorizedError;
import com.storeops.common.error.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ErrorsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private void assertError(AppError error, String code, int status) {
        assertEquals(code, error.getCode());
        assertEquals(status, error.getStatusCode());
    }

    @Test
    void everyErrorCarriesCodeMessageAndStatus() {
        assertError(new NotFoundError("activity", "a-1"), "NOT_FOUND", 404);
        assertError(new ValidationError("bad"), "VALIDATION_ERROR", 400);
        assertError(new UnauthorizedError("who"), "UNAUTHORIZED", 401);
        assertError(new ForbiddenError("no"), "FORBIDDEN", 403);
        assertError(new ConflictError("dup"), "CONFLICT", 409);
        assertEquals("activity not found: a-1", new NotFoundError("activity", "a-1").getMessage());
    }

    @Test
    void handlerMapsAppErrorsToTheirStatus() {
        ResponseEntity<ApiError> response = handler.handleAppError(new ConflictError("dup"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(new ApiError("CONFLICT", "dup"), response.getBody());
    }

    @Test
    void handlerKeepsFrameworkStatusCodes() {
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("HTTP_404", response.getBody().code());
    }

    @Test
    void handlerHidesUnexpectedFailureDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new UnsupportedOperationException("secret"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("unexpected server error", response.getBody().message());
    }
}
