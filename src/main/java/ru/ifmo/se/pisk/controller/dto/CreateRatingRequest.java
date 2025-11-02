package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRatingRequest(
        @NotNull Long userId,
        @Min(1) @Max(5) int value,
        @Size(max = 512) String comment
) {
}
