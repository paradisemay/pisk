package ru.ifmo.se.pisk.controller.dto;

import java.time.Instant;

public record TrackResponse(
        Long id,
        String name,
        Long authorId,
        Long albumId,
        Long artistId,
        Long genreId,
        String filepath,
        String coverPath,
        int numberOnAlbum,
        boolean explicit,
        boolean inoagent,
        long durationSeconds,
        long filesize,
        Instant uploadDate,
        double averageRating,
        int rateCount,
        long likeCount,
        long commentCount
) {
}
