package ru.ifmo.se.pisk.domain;

import java.time.Instant;
import java.util.Map;

public record AuditRecord(
        Long id,
        AuditAction action,
        String entityType,
        String entityId,
        Long actorId,
        Instant occurredAt,
        Map<String, String> metadata
) {
}
