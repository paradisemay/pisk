package ru.ifmo.se.pisk.domain;

import java.time.Instant;

public record TrackInfo(
        Long id,
        String name,
        Long authorId,
        Long albumId,
        Long artistId,
        Long genreId,
        String filepath,
        String coverPath,
        String contentType,
        String originalFilename,
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
