package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateAlbumRequest(
        @NotNull Long ownerId,
        @NotBlank @Size(max = 128) String title,
        @Size(max = 512) String description,
        @NotNull LocalDate releaseDate,
        @NotNull Long genreId,
        @NotNull Long labelId,
        @NotNull @Min(1) Integer trackNumber,
        @NotNull Boolean explicit,
        @NotNull Boolean inoagent,
        String coverUrl
) {
}
