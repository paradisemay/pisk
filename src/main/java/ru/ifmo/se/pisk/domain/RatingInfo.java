package ru.ifmo.se.pisk.domain;

import java.time.Instant;

public record RatingInfo(
        Long id,
        Long trackId,
        Long userId,
        int value,
        String comment,
        Instant createdAt
) {
}
