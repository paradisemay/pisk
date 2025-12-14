package ru.ifmo.se.pisk.web.view;

import java.time.Instant;

public record TrackDetailsView(
        Long id,
        String name,
        Long authorId,
        String authorName,
        String genreTitle,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        int rateCount,
        long durationSeconds,
        long fileSize,
        Instant uploadDate,
        String downloadUrl
) {
}
