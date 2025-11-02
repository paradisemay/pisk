package ru.ifmo.se.pisk.controller.dto;

import java.time.Instant;
import java.util.Map;
import ru.ifmo.se.pisk.domain.AuditAction;

public record AuditResponse(
        Long id,
        AuditAction action,
        String entityType,
        String entityId,
        Long actorId,
        Instant occurredAt,
        Map<String, String> metadata
) {
}
