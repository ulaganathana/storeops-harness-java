package com.storeops.common.error;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppError.class)
    public ResponseEntity<ApiError> handleAppError(AppError error) {
        return respond(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respond(new ValidationError(details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception) {
        return respond(new ValidationError("request body or parameter is malformed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse frameworkError) {
            String detail = frameworkError.getBody().getDetail();
            ApiError body = new ApiError("HTTP_" + frameworkError.getStatusCode().value(),
                    detail == null ? "request failed" : detail);
            return ResponseEntity.status(frameworkError.getStatusCode()).body(body);
        }
        LOG.error("Unexpected error", exception);
        return ResponseEntity.status(500).body(new ApiError("INTERNAL_ERROR", "unexpected server error"));
    }

    private ResponseEntity<ApiError> respond(AppError error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(new ApiError(error.getCode(), error.getMessage()));
    }
}
