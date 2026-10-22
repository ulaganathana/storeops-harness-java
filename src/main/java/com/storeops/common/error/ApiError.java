package com.storeops.common.error;

/** JSON error body returned for every failed request. */
public record ApiError(String code, String message) {
}
