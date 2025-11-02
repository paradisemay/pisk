package ru.ifmo.se.pisk.controller.dto;

import java.time.LocalDate;
import java.util.Set;

public record AlbumResponse(
        Long id,
        String title,
        String description,
        LocalDate releaseDate,
        Long ownerId,
        Set<Long> genreIds,
        String coverUrl
) {
}
