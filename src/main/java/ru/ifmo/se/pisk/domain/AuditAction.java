package ru.ifmo.se.pisk.domain;

public enum AuditAction {
    USER_REGISTERED,
    TRACK_UPLOADED,
    TRACK_STATUS_CHANGED,
    RATING_CREATED,
    SUBSCRIPTION_UPDATED,
    LOW_RATING_RECEIVED,
    /**
     * @deprecated These complaint-related actions are retained to allow hydrating
     * persisted audit records that still reference them.
     */
    @Deprecated
    COMPLAINT_FILED,
    /**
     * @deprecated These complaint-related actions are retained to allow hydrating
     * persisted audit records that still reference them.
     */
    @Deprecated
    COMPLAINT_RESOLVED,
    RATE_LIMIT_TRIGGERED
}
