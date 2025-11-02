package ru.ifmo.se.pisk.domain;

import java.time.Instant;

public record ComplaintInfo(
        Long id,
        Long trackId,
        Long complainantId,
        String reason,
        ComplaintStatus status,
        Instant createdAt,
        Instant resolvedAt,
        String moderatorNote
) {
}
