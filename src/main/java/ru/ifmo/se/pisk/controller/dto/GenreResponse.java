package ru.ifmo.se.pisk.controller.dto;

public record GenreResponse(
        Long id,
        String title,
        String description,
        String color,
        double averageRating
) {
}
