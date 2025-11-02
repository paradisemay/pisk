package ru.ifmo.se.pisk.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public record TrackInfo(
        Long id,
        String title,
        Duration duration,
        TrackStatus status,
        Long albumId,
        Long artistId,
        Set<Long> genreIds,
        String storagePath,
        String contentType,
        long fileSize,
        Instant uploadedAt,
        Instant publishedAt,
        double averageRating,
        long ratingsCount,
        long playCount
) {
}
