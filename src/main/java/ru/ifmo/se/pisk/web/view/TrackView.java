package ru.ifmo.se.pisk.web.view;

import java.time.Instant;

public record TrackView(
        Long id,
        String name,
        Long authorId,
        String authorName,
        String genreTitle,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        long durationSeconds,
        Instant uploadDate
) {
}
