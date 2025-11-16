package ru.ifmo.se.pisk.domain;

import java.time.Instant;

public record AlbumInfo(
        Long id,
        String name,
        String description,
        Instant uploadDate,
        String coverUrl,
        Long genreId,
        int trackNumber,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        Long labelId,
        Instant publicationDate
) {
}
