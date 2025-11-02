package ru.ifmo.se.pisk.controller.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import ru.ifmo.se.pisk.domain.TrackStatus;

public record TrackResponse(
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
