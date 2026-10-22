package com.storeops.activities.model;

public record BulkStatusItemResult(
        String activityId,
        String outcome,
        String errorCode,
        String message) {

    public static BulkStatusItemResult succeeded(String activityId) {
        return new BulkStatusItemResult(activityId, "SUCCEEDED", null, null);
    }

    public static BulkStatusItemResult failed(String activityId, String code, String message) {
        return new BulkStatusItemResult(activityId, "FAILED", code, message);
    }
}
