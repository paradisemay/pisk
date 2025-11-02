package ru.ifmo.se.pisk.domain;

import java.time.LocalDate;
import java.util.Set;

public record AlbumInfo(
        Long id,
        String title,
        String description,
        LocalDate releaseDate,
        Long ownerId,
        Set<Long> genreIds,
        String coverUrl
) {
}
