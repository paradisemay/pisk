package ru.ifmo.se.pisk.service.dto;

public record RatingCommand(
        Long trackId,
        Long userId,
        int value,
        String comment
) {
}
