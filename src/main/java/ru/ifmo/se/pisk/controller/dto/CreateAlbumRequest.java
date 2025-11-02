package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record CreateAlbumRequest(
        @NotNull Long ownerId,
        @NotBlank @Size(max = 128) String title,
        @Size(max = 512) String description,
        LocalDate releaseDate,
        Set<Long> genreIds,
        String coverUrl
) {
}
