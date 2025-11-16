package ru.ifmo.se.pisk.controller.dto;

public record AlbumResponse(
        Long id,
        String name,
        String description,
        java.time.Instant uploadDate,
        String coverUrl,
        Long genreId,
        int trackNumber,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        Long labelId,
        java.time.Instant publicationDate
) {
}
