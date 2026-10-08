package com.storeops.shared.event;

/** Registry of event type names. New cross-module events MUST be added here. */
public final class EventTypes {
    public static final String ACTIVITY_CREATED = "ACTIVITY_CREATED";
    public static final String ACTIVITY_STATUS_CHANGED = "ACTIVITY_STATUS_CHANGED";
    public static final String PROGRAMME_CREATED = "PROGRAMME_CREATED";
    public static final String MEMBER_ADDED = "MEMBER_ADDED";
    public static final String SLA_BREACH = "SLA_BREACH";

    private EventTypes() { }
}
