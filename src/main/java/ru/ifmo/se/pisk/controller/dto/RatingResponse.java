package ru.ifmo.se.pisk.controller.dto;

import java.time.Instant;

public record RatingResponse(
        Long id,
        Long trackId,
        Long userId,
        int value,
        String comment,
        Instant createdAt
) {
}
