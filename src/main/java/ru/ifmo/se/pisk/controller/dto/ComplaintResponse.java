package ru.ifmo.se.pisk.controller.dto;

import java.time.Instant;
import ru.ifmo.se.pisk.domain.ComplaintStatus;

public record ComplaintResponse(
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
